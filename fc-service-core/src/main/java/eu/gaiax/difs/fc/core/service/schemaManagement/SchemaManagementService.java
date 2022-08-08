package eu.gaiax.difs.fc.core.service.schemaManagement;

import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

public interface SchemaManagementService {

  public enum SchemaType {
    ONTOLOGY,
    SHAPE,
    VOCABULARY
  }

  /**
   * verify if a given schema is syntactically correct
   *
   * @param schema that can be shacl (ttl), vocabulary(SKOS), ontology(owl), and
   * needs to be verified
   * @return TRUE if the schema is syntactically valid
   */
  public boolean verifySchema(String schema);

  /**
   * store a schema after has been successfully verified for its type and syntax
   *
   * @param schema to be stored
   * @return The internal identifier of the Schema
   */
  public String addSchema(String schema);

  /**
   * Update the schema with the given identifier.
   *
   * @param identifier The identifier of the schema to update.
   * @param schema The content to replace the schema with.
   */
  public void updateSchema(String identifier, String schema);

  /**
   * Delete the schema with the given identifier.
   *
   * @param identifier The identifier of the schema to delete.
   */
  public void deleteSchema(String identifier);

  /**
   * Get the identifiers of all schemas, sorted by schema type.
   *
   * @return the identifiers of all schemas, sorted by schema type.
   */
  public Map<SchemaType, List<String>> getSchemaList();

  /**
   * Get the content of the schema with the given identifier.
   *
   * @param identifier The identifier of the schema to return.
   * @return The schema content.
   */
  public String getSchema(String identifier);

  /**
   * Get the schemas that defines the given entity, grouped by schema type.
   *
   * @param entity The entity to get the defining schemas for.
   * @return the identifiers of the defining schemas, sorted by schema type.
   */
  public Map<SchemaType, List<String>> getSchemaForType(String entity);

  /**
   * Get the union schema.
   *
   * @return The union RDF graph.
   */
  public Object getCompositeSchema();

}
