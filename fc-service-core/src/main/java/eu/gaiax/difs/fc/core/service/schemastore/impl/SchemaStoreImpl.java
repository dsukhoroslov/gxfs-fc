package eu.gaiax.difs.fc.core.service.schemastore.impl;

import com.google.common.base.Strings;
import eu.gaiax.difs.fc.core.exception.ConflictException;
import eu.gaiax.difs.fc.core.exception.NotFoundException;
import eu.gaiax.difs.fc.core.exception.ServerException;
import eu.gaiax.difs.fc.core.exception.VerificationException;
import eu.gaiax.difs.fc.core.pojo.ContentAccessor;
import eu.gaiax.difs.fc.core.pojo.ContentAccessorDirect;
import eu.gaiax.difs.fc.core.pojo.ContentAccessorFile;
import eu.gaiax.difs.fc.core.service.filestore.FileStore;
import eu.gaiax.difs.fc.core.service.schemastore.SchemaStore;
import eu.gaiax.difs.fc.core.util.HashUtils;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.persistence.EntityExistsException;
import javax.persistence.LockModeType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.apache.jena.shacl.vocabulary.SHACLM;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.OWL2;

/**
 *
 */
@Component
@Transactional
@Slf4j
public class SchemaStoreImpl implements SchemaStore {

  @Autowired
  @Qualifier("schemaFileStore")
  private FileStore fileStore;

  @Autowired
  private SessionFactory sessionFactory;

  private static final Map<SchemaType, ContentAccessor> COMPOSITE_SCHEMAS = new ConcurrentHashMap<>();

  @Override
  public void initializeDefaultSchemas() {
    Session currentSession = sessionFactory.getCurrentSession();
    long count = currentSession.createQuery("select count(s) from SchemaRecord s", Long.class).getSingleResult();
    if (count > 0) {
      log.info("initializeDefaultSchemas; {} default schemas already initialized", count);
      return;
    }
    
    try {
      count += addSchemasFromDirectory("defaultschema/ontology");
      count += addSchemasFromDirectory("defaultschema/shacl");
      log.info("initializeDefaultSchemas; added {} default schemas", count);
      currentSession.flush();
      count = currentSession.createQuery("select count(s) from SchemaRecord s", Long.class).getSingleResult();
      log.info("initializeDefaultSchemas; {} default schemas found in DB", count);
    } catch (Exception ex) {
      ex.printStackTrace();
      //throw ex;
    }
  }

  //private void addSchemasFromDirectory(String path) throws IOException {
  //  URL url = getClass().getClassLoader().getResource(path);
  //  String str = URLDecoder.decode(url.getFile(), StandardCharsets.UTF_8);
  //  File ontologyDir = new File(str);
  //  for (File ontology : ontologyDir.listFiles()) {
  //    log.debug("addSchemasFromDirectory; Adding schema: {}", ontology);
  //    addSchema(new ContentAccessorFile(ontology));
  //  }
  //}

  private int addSchemasFromDirectory(String path) throws IOException {
    PathMatchingResourcePatternResolver scanner = new PathMatchingResourcePatternResolver();
    org.springframework.core.io.Resource[] resources = scanner.getResources(path + "/*");
    int cnt = 0;
    for (org.springframework.core.io.Resource resource: resources) {
      log.debug("addSchemasFromDirectory; Adding schema: {}", resource.getFilename());
      String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      addSchema(new ContentAccessorDirect(content));
      cnt++;
    }
    return cnt;
  }
  
  /**
   * Analyse the given schema character content.
   *
   * @param schema The schema to analyse.
   * @return The analysis results.
   */
  public SchemaAnalysisResult analyseSchema(ContentAccessor schema) {
    SchemaAnalysisResult result = new SchemaAnalysisResult();
    Set<String> extractedUrlsSet = new HashSet<>();
    Model model = ModelFactory.createDefaultModel();

    // TODO: calc schemaType from content file extension
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
    // what if result is not valid here?
    if (!result.isValid()) {
      return result;  
    }
    
    if (model.contains(null, RDF.type, SHACLM.NodeShape) || model.contains(null, RDF.type, SHACLM.PropertyShape)) {
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
          result.setErrorMessage("Schema is not supported");
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
      List<String> extractedUrls = new ArrayList<>(extractedUrlsSet);
      result.setExtractedUrls(extractedUrls);
    }
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
    SchemaAnalysisResult result = analyseSchema(schema);
    if (result.getSchemaType().equals(type)) {
      return true;
    } else {
      return false;
    }
  }

  private ContentAccessor createCompositeSchema(SchemaType type) {
    log.debug("createCompositeSchema.enter; got type: {}", type);

    StringWriter out = new StringWriter();
    Map<SchemaType, List<String>> schemaList = getSchemaList();
    log.debug("createCompositeSchema; got schemaList: {}", schemaList);

    Model model = ModelFactory.createDefaultModel();
    Model unionModel = ModelFactory.createDefaultModel();
    List<String> schemaListForType = schemaList.get(type);
    if (schemaListForType == null) {
      return new ContentAccessorDirect("");
    }
    for (String schemaId : schemaListForType) {
      ContentAccessor schemaContent = getSchema(schemaId);
      StringReader schemaContentReader = new StringReader(schemaContent.getContentAsString());
      model.read(schemaContentReader, "", "TURTLE");
      unionModel.add(model);
    }
    RDFDataMgr.write(out, unionModel, Lang.TURTLE);
    ContentAccessor content = new ContentAccessorDirect(out.toString());

    try {
      final String compositeSchemaName = "CompositeSchema" + type.name();
      fileStore.replaceFile(compositeSchemaName, content);
      content = fileStore.readFile(compositeSchemaName);
      COMPOSITE_SCHEMAS.put(type, content);
      log.debug("createCompositeSchema.exit; returning: {}", content.getContentAsString().length());
    } catch (IOException ex) {
      log.error("createCompositeSchema.error", ex);
    }
    return content;
  }

  @Override
  public boolean verifySchema(ContentAccessor schema) {
    SchemaAnalysisResult result = analyseSchema(schema);
    return result.isValid();
  }

  @Override
  public String addSchema(ContentAccessor schema) {
    SchemaAnalysisResult result = analyseSchema(schema);
    if (!result.isValid()) {
      throw new VerificationException("Schema is not valid: " + result.getErrorMessage());
    }
    String schemaId = result.getExtractedId();
    String nameHash;
    String content = schema.getContentAsString();
    if (Strings.isNullOrEmpty(schemaId)) {
      nameHash = HashUtils.calculateSha256AsHex(content);
      schemaId = nameHash;
      result.setExtractedId(schemaId);
    } else {
      nameHash = HashUtils.calculateSha256AsHex(schemaId);
    }

    Session currentSession = sessionFactory.getCurrentSession();

    // Check duplicate terms
    List<SchemaTerm> redefines = currentSession.byMultipleIds(SchemaTerm.class)
        .multiLoad(result.getExtractedUrls());
    redefines = redefines.stream()
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    if (!redefines.isEmpty()) {
      throw new VerificationException("Schema redefines " + redefines.size() + " terms. First: " + redefines.get(0));
    }

    SchemaRecord newRecord = new SchemaRecord(schemaId, nameHash, result.getSchemaType(), content, result.getExtractedUrls());
    try {
      currentSession.persist(newRecord);
    } catch (EntityExistsException ex) {
      throw new ConflictException("A schema with id " + schemaId + " already exists.");
    }

    try {
      fileStore.storeFile(nameHash, schema);
    } catch (IOException ex) {
      throw new RuntimeException("Failed to store schema file", ex);
    }

    currentSession.flush();
    COMPOSITE_SCHEMAS.remove(result.getSchemaType());
    // TODO: Re-Validate SDs in a separate thread.
    return schemaId;
  }

  @Override
  public void updateSchema(String identifier, ContentAccessor schema) {
    SchemaAnalysisResult result = analyseSchema(schema);
    String schemaId = result.getExtractedId();
    if (!result.isValid()) {
      throw new VerificationException("Schema is not valid.");
    }
    if (schemaId != null && !schemaId.equals(identifier)) {
      throw new IllegalArgumentException("Given schema does not have the same Identifier as the old schema: " + identifier + " <> " + schemaId);
    }
    Session currentSession = sessionFactory.getCurrentSession();
    Transaction transaction = currentSession.getTransaction();

    // Find and lock record.
    SchemaRecord existing = currentSession.find(SchemaRecord.class, identifier, LockModeType.PESSIMISTIC_WRITE);

    if (existing == null) {
      currentSession.clear();
      throw new NotFoundException("Schema with id " + identifier + " was not found");
    }

    // Remove old terms
    CriteriaBuilder cb = currentSession.getCriteriaBuilder();
    CriteriaDelete<SchemaTerm> delete = cb.createCriteriaDelete(SchemaTerm.class);
    delete.where(cb.equal(delete.from(SchemaTerm.class).get("schemaId"), identifier));
    currentSession.createQuery(delete).executeUpdate();

    // Check duplicate terms
    List<SchemaTerm> redefines = currentSession.byMultipleIds(SchemaTerm.class)
        .multiLoad(result.getExtractedUrls());
    redefines = redefines.stream()
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    if (!redefines.isEmpty()) {
      currentSession.clear();
      throw new ConflictException("Schema redefines " + redefines.size() + " terms. First: " + redefines.get(0));
    }

    existing.setUpdateTime(Instant.now());
    existing.replaceTerms(result.getExtractedUrls());
    existing.setContent(schema.getContentAsString());
    try {
      currentSession.update(existing);
    } catch (EntityExistsException ex) {
      transaction.rollback();
      throw new ConflictException("Schema redefines terms.");
    }
    try {
      //Update schema file
      fileStore.replaceFile(existing.getNameHash(), schema);
    } catch (IOException ex) {
      transaction.rollback();
      throw new RuntimeException("Failed to store schema file", ex);
    }
    currentSession.flush();
    COMPOSITE_SCHEMAS.remove(result.getSchemaType());

    // TODO: Re-Validate SDs in a separate thread.
  }

  @Override
  public void deleteSchema(String identifier) {
    Session currentSession = sessionFactory.getCurrentSession();
    // Find and lock record.
    SchemaRecord existing = currentSession.find(SchemaRecord.class, identifier, LockModeType.PESSIMISTIC_WRITE);
    if (existing == null) {
      throw new NotFoundException("Schema with id " + identifier + " was not found");
    }
    currentSession.delete(existing);
    try {
      fileStore.deleteFile(existing.getNameHash());
    } catch (IOException ex) {
      currentSession.clear();
      throw new ServerException("Failed to delete schema from file store. (" + ex.getMessage() + ")");
    }
    currentSession.flush();
    COMPOSITE_SCHEMAS.remove(existing.getType());
  }

  @Override
  public Map<SchemaType, List<String>> getSchemaList() {
    Session currentSession = sessionFactory.getCurrentSession();
    Map<SchemaType, List<String>> result = new HashMap<>();
    currentSession.createQuery("select new eu.gaiax.difs.fc.core.service.schemastore.impl.SchemaTypeIdPair(s.type, s.schemaId) from SchemaRecord s", SchemaTypeIdPair.class)
        .stream().forEach(p -> result.computeIfAbsent(p.getType(), t -> new ArrayList<>()).add(p.getSchemaId()));
    // TORemove
    log.debug("getSchemaList; got schemaList: {}", result);
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
    try {
      return fileStore.readFile(existing.getNameHash());

    } catch (IOException ex) {
      throw new ServerException("File for Schema " + identifier + " does not exist.");
    }
  }

  @Override
  public Map<SchemaType, List<String>> getSchemasForTerm(String entity) {
    Session currentSession = sessionFactory.getCurrentSession();
    Map<SchemaType, List<String>> result = new HashMap<>();
    currentSession.createQuery("select new eu.gaiax.difs.fc.core.service.schemastore.impl.SchemaTypeIdPair(s.type, s.schemaId) from SchemaRecord s join s.terms as t where t.term=?1", SchemaTypeIdPair.class)
        .setParameter(1, entity)
        .stream().forEach(p -> result.computeIfAbsent(p.getType(), t -> new ArrayList<>()).add(p.getSchemaId()));
    return result;
  }

  @Override
  public ContentAccessor getCompositeSchema(SchemaType type) {
    try {
      ContentAccessor composite = COMPOSITE_SCHEMAS.get(type);
      if (composite == null) {
        composite = createCompositeSchema(type);
      }
      return composite;
    } catch (Exception ex) {
      log.error("getCompositeSchema.error", ex);
      throw new ServerException("Error returning composite schema of type " + type, ex);
    }
  }

}
