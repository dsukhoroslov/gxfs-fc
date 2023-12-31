package eu.gaiax.difs.fc.core.service.schemastore.impl;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shacl.vocabulary.SHACLM;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Strings;

import eu.gaiax.difs.fc.core.exception.ConflictException;
import eu.gaiax.difs.fc.core.exception.NotFoundException;
import eu.gaiax.difs.fc.core.exception.ServerException;
import eu.gaiax.difs.fc.core.exception.VerificationException;
import eu.gaiax.difs.fc.core.pojo.ContentAccessor;
import eu.gaiax.difs.fc.core.pojo.ContentAccessorDirect;
import eu.gaiax.difs.fc.core.service.filestore.FileStore;
import eu.gaiax.difs.fc.core.service.schemastore.SchemaStore;
import eu.gaiax.difs.fc.core.util.HashUtils;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.LockModeType;
import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
@Component
@Transactional
public class SchemaStoreImpl implements SchemaStore {

  @Autowired
  @Qualifier("schemaFileStore")
  private FileStore fileStore;

  @Autowired
  private SessionFactory sessionFactory;

  private static final Map<SchemaType, ContentAccessor> COMPOSITE_SCHEMAS = new ConcurrentHashMap<>();


  @Override
  public int initializeDefaultSchemas() {
    Transaction tx = null;  
    try (Session session = sessionFactory.openSession()) {
      log.debug("initializeDefaultSchemas; got session: {}", session);
      int count = 0;
      Long found = session.createQuery("select count(s) from SchemaRecord s", Long.class).getSingleResult();
      if (found == 0) {
        tx = session.beginTransaction();
        
        count += addSchemasFromDirectory("defaultschema/ontology", session);
        count += addSchemasFromDirectory("defaultschema/shacl", session);
        session.flush();
        log.info("initializeDefaultSchemas; added {} default schemas", count);
        found = session.createQuery("select count(s) from SchemaRecord s", Long.class).getSingleResult(); // it returns 0 for some reason
        tx.commit();
      }
      log.info("initializeDefaultSchemas; {} schemas found in Schema DB", found);
      return count;
    } catch (Exception ex) {
      log.error("initializeDefaultSchemas.error", ex);
      if (tx != null) {
        tx.rollback();
      }
      throw new ServerException(ex);
    }
  }  
  
  private int addSchemasFromDirectory(String path, Session session) throws IOException {
    PathMatchingResourcePatternResolver scanner = new PathMatchingResourcePatternResolver();
    org.springframework.core.io.Resource[] resources = scanner.getResources(path + "/*");
    int cnt = 0;
    for (org.springframework.core.io.Resource resource: resources) {
      log.debug("addSchemasFromDirectory; Adding schema: {}", resource.getFilename());
      String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      addSchema(new ContentAccessorDirect(content), session);
      cnt++;
    }
    return cnt;
  }

  /**
   * Analyze the given schema character content.
   *
   * @param schema The schema to analyse.
   * @return The analysis results.
   */
  public SchemaAnalysisResult analyzeSchema(ContentAccessor schema) {
    SchemaAnalysisResult result = new SchemaAnalysisResult();
    Set<String> extractedUrlsSet = new HashSet<>();
    Model model = ModelFactory.createDefaultModel();

    List<String> schemaType = Arrays.asList("JSON-LD", "RDF/XML", "TTL");
    for (String type : schemaType) {
      try {
        model.read(schema.getContentAsStream(), null, type);
        result.setValid(true);
        break;
      } catch (Exception exc) {
        result.setValid(false);
        result.setErrorMessage(exc.getMessage());
      }
    }
    if (model.contains(null, RDF.type, SHACLM.NodeShape)
        || model.contains(null, RDF.type, SHACLM.PropertyShape)) {
      result.setSchemaType(SchemaType.SHAPE);
      result.setExtractedId(null);
    } else {
      ResIterator resIteratorProperty = model.listResourcesWithProperty(RDF.type, OWL.Ontology);
      if (resIteratorProperty.hasNext()) {
        Resource resource = resIteratorProperty.nextResource();
        result.setSchemaType(SchemaType.ONTOLOGY);
        result.setExtractedId(resource.getURI());
        if (resIteratorProperty.hasNext()) {
          result.setErrorMessage("Ontology Schema has multiple Ontology IRIs");
          result.setExtractedId(null);
          result.setValid(false);
        }
      } else {
        resIteratorProperty = model.listResourcesWithProperty(RDF.type, SKOS.ConceptScheme);
        if (resIteratorProperty.hasNext()) {
          Resource resource = resIteratorProperty.nextResource();
          result.setSchemaType(SchemaType.VOCABULARY);
          result.setExtractedId(resource.getURI());
          if (resIteratorProperty.hasNext()) {
            result.setErrorMessage("Vocabulary contains multiple concept schemes");
            result.setExtractedId(null);
            result.setValid(false);
          }
        } else {
          result.setValid(false);
          result.setErrorMessage("Schema type not supported");
        }
      }
    }
    if (result.isValid()) {
      switch (result.getSchemaType()) {
        case SHAPE:
          addExtractedUrls(model, SHACLM.NodeShape, extractedUrlsSet);
          addExtractedUrls(model, SHACLM.PropertyShape, extractedUrlsSet);
          break;

        case ONTOLOGY:
          addExtractedUrls(model, OWL2.NamedIndividual, extractedUrlsSet);
          addExtractedUrls(model, RDF.Property, extractedUrlsSet);
          addExtractedUrls(model, OWL2.DatatypeProperty, extractedUrlsSet);
          addExtractedUrls(model, OWL2.ObjectProperty, extractedUrlsSet);
          addExtractedUrls(model, RDFS.Class, extractedUrlsSet);
          addExtractedUrls(model, OWL2.Class, extractedUrlsSet);
          break;

        case VOCABULARY:
          addExtractedUrls(model, SKOS.Concept, extractedUrlsSet);
          break;
        default:
        // this will not happen
      }
    }
    result.setExtractedUrls(extractedUrlsSet);
    return result;
  }

  public void addExtractedUrls(Model model, RDFNode node, Set<String> extractedSet) {
    ResIterator resIteratorNode = model.listResourcesWithProperty(RDF.type, node);
    while (resIteratorNode.hasNext()) {
      Resource rs = resIteratorNode.nextResource();
      extractedSet.add(rs.getURI());
    }
  }

  public boolean isSchemaType(ContentAccessor schema, SchemaType type) {
    SchemaAnalysisResult result = analyzeSchema(schema);
    return result.getSchemaType().equals(type);
  }

  private ContentAccessor createCompositeSchema(SchemaType type) {
    log.debug("createCompositeSchema.enter; got type: {}", type);

    StringWriter out = new StringWriter();
    Map<SchemaType, List<String>> schemaList = getSchemaList();

    List<String> schemaListForType = schemaList.get(type);
    if (schemaListForType == null) {
      log.debug("createCompositeSchema.exit; returning empty content for unknown type");
      return new ContentAccessorDirect("");
    }
    
    Model model = ModelFactory.createDefaultModel();
    Model unionModel = ModelFactory.createDefaultModel();
    for (String schemaId : schemaListForType) {
      ContentAccessor schemaContent = getSchema(schemaId);
      StringReader schemaContentReader = new StringReader(schemaContent.getContentAsString());
      model.read(schemaContentReader, null, "TURTLE");
      unionModel.add(model);
    }
    RDFDataMgr.write(out, unionModel, Lang.TURTLE);
    ContentAccessor content = new ContentAccessorDirect(out.toString());

    log.debug("createCompositeSchema.exit; returning: {}", content.getContentAsString().length());
    try {
      final String compositeSchemaName = "CompositeSchema" + type.name();
      fileStore.replaceFile(compositeSchemaName, content);
      // the ContentAccessor returned from this function is cached until this instance gets a schema change. 
      // That's why it is important that the file-based ContentAccessor is returned, and not the String-based one. 
      // Otherwise, if another instance changes the file because that other instance received a schema change, this instance will 
      // never serve the new content, since it cached the String-based content.
      // By returning the file-based ContentAccessor, a change of the file will automatically update the content that all instances serve.
      content = fileStore.readFile(compositeSchemaName);
    } catch (IOException ex) {
      log.error("createCompositeSchema.error: Failed to store composite schema", ex);
    }
    return content;
  }

  @Override
  public boolean verifySchema(ContentAccessor schema) {
    SchemaAnalysisResult result = analyzeSchema(schema);
    return result.isValid();
  }

  @Override
  public String addSchema(ContentAccessor schema) {
    Session currentSession = sessionFactory.getCurrentSession();
    log.debug("addSchema; current session: {}", currentSession);
    return addSchema(schema, currentSession);
  }
    
  private String addSchema(ContentAccessor schema, Session currentSession) {
	  
	SchemaAnalysisResult result = analyzeSchema(schema);
    if (!result.isValid()) {
      throw new VerificationException("Schema is not valid: " + result.getErrorMessage());
    }
    
    String schemaId = result.getExtractedId();
    String nameHash;
    if (Strings.isNullOrEmpty(schemaId)) {
      nameHash = HashUtils.calculateSha256AsHex(schema.getContentAsString());
      schemaId = nameHash;
      result.setExtractedId(schemaId);
    } else {
      nameHash = HashUtils.calculateSha256AsHex(schemaId);
    }

    // Check duplicate terms
    List<SchemaTerm> redefines = currentSession.byMultipleIds(SchemaTerm.class)
        .multiLoad(new ArrayList<>(result.getExtractedUrls()));
    redefines = redefines.stream()
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    if (!redefines.isEmpty()) {
      throw new ConflictException("Schema redefines " + redefines.size() + " terms. First: " + redefines.get(0));
    }

    SchemaRecord newRecord = new SchemaRecord(schemaId, nameHash, result.getSchemaType(), schema.getContentAsString(),
        result.getExtractedUrls());
    try {
      log.debug("addSchema; SchemaId: {}, terms count: {}", schemaId, newRecord.getTerms().size());
      currentSession.persist(newRecord);
      currentSession.flush();
    } catch (EntityExistsException ex) {
      throw new ConflictException("A schema with id " + schemaId + " already exists.");
    }

    COMPOSITE_SCHEMAS.remove(result.getSchemaType());
    return schemaId;
  }

  @Override
  public void updateSchema(String identifier, ContentAccessor schema) {
    SchemaAnalysisResult result = analyzeSchema(schema);
    String schemaId = result.getExtractedId();
    if (!result.isValid()) {
      throw new VerificationException("Schema is not valid.");
    }
    if (schemaId != null && !schemaId.equals(identifier)) {
      throw new IllegalArgumentException("Given schema does not have the same Identifier as the old schema: " + identifier + " <> " + schemaId);
    }
    Session currentSession = sessionFactory.getCurrentSession();

    // Find and lock record.
    SchemaRecord existing = currentSession.find(SchemaRecord.class, identifier, LockModeType.PESSIMISTIC_WRITE);

    if (existing == null) {
      currentSession.clear();
      throw new NotFoundException("Schema with id " + identifier + " was not found");
    }

    // Remove old terms
    int deleted = currentSession.createMutationQuery("delete from SchemaTerm t where t.schemaId = :schemaid")
        .setParameter("schemaid", identifier)
        .executeUpdate();
    log.debug("updateSchema; Deleted {} terms", deleted);
    existing.getTerms().forEach(t -> currentSession.detach(t));

    // Check duplicate terms
    List<SchemaTerm> redefines = currentSession.byMultipleIds(SchemaTerm.class)
        .multiLoad(new ArrayList<>(result.getExtractedUrls()));
    redefines = redefines.stream()
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    if (!redefines.isEmpty()) {
      currentSession.clear();
      throw new ConflictException("Schema redefines " + redefines.size() + " terms. First: " + redefines.get(0));
    }

    existing.setUpdateTime(Instant.now());
    existing.replaceTerms(new ArrayList<>(result.getExtractedUrls()));
    existing.setContent(schema.getContentAsString());
    try {
      currentSession.merge(existing);
      currentSession.flush();
    } catch (EntityExistsException ex) {
      throw new ConflictException("Schema redefines terms.");
    }
    COMPOSITE_SCHEMAS.remove(result.getSchemaType());
    // SDs will be revalidated in a separate thread.
  }

  @Override
  public void deleteSchema(String identifier) {
    Session currentSession = sessionFactory.getCurrentSession();
    Query<Integer> q = currentSession.createNativeQuery("delete from schemafiles where schemaid = :schemaid returning type", Integer.class);
    q.setParameter("schemaid", identifier);
    List<Integer> result = q.list();
    log.debug("deleteSchema; delete result: {}", result);
    if (result.size() == 0) {
      throw new NotFoundException("Schema with id " + identifier + " was not found");
    }
    int type = result.get(0);
    currentSession.flush();
    COMPOSITE_SCHEMAS.remove(SchemaType.values()[type]);
  }

  @Override
  public Map<SchemaType, List<String>> getSchemaList() {
    Session currentSession = sessionFactory.getCurrentSession();
    Map<SchemaType, List<String>> result = new HashMap<>();
    currentSession.createQuery("select new eu.gaiax.difs.fc.core.service.schemastore.impl.SchemaTypeRecord(s.type, s.schemaId) from SchemaRecord s", 
    		SchemaTypeRecord.class)
        .stream().forEach(p -> result.computeIfAbsent(p.type(), t -> new ArrayList<>()).add(p.schemaId()));
    return result;
  }

  @Override
  public ContentAccessor getSchema(String identifier) {
    Session currentSession = sessionFactory.getCurrentSession();
    // Find and lock record.
    SchemaRecord existing = currentSession.find(SchemaRecord.class, identifier);
    if (existing == null) {
      throw new NotFoundException("Schema with id " + identifier + " was not found");
    }
    return new ContentAccessorDirect(existing.getContent());
  }

  @Override
  public Map<SchemaType, List<String>> getSchemasForTerm(String entity) {
    Session currentSession = sessionFactory.getCurrentSession();
    Map<SchemaType, List<String>> result = new HashMap<>();
    currentSession.createQuery("select new eu.gaiax.difs.fc.core.service.schemastore.impl.SchemaTypeRecord(s.type, s.schemaId) " + 
    		"from SchemaRecord s join s.terms as t where t.term=?1", SchemaTypeRecord.class)
        .setParameter(1, entity)
        .stream().forEach(p -> result.computeIfAbsent(p.type(), t -> new ArrayList<>()).add(p.schemaId()));
    return result;
  }

  @Override
  public ContentAccessor getCompositeSchema(SchemaType type) {
    return COMPOSITE_SCHEMAS.computeIfAbsent(type, t -> createCompositeSchema(t));
  }

  @Override
  public void clear() {
    try ( Session session = sessionFactory.openSession()) {
      Transaction transaction = session.getTransaction();
      // Transaction is sometimes not active. For instance when called from an @AfterAll Test method
      if (transaction == null || !transaction.isActive()) {
        transaction = session.beginTransaction();
        session.createMutationQuery("delete from SchemaRecord").executeUpdate();
        transaction.commit();
      } else {
        session.createMutationQuery("delete from SchemaRecord").executeUpdate();
      }
    } catch (Exception ex) {
      log.error("SchemaStoreImpl: Exception while clearing Database.", ex);
    }
    try {
      fileStore.clearStorage();
    } catch (IOException ex) {
      log.error("SchemaStoreImpl: Exception while clearing FileStore: {}.", ex.getMessage());
    }
    COMPOSITE_SCHEMAS.clear();
  }

}
