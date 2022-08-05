package eu.gaiax.difs.fc.core.service.schemaManagement;

import org.springframework.web.multipart.MultipartFile;

public interface SchemaManagementService {

    /**
     * verify if a given schema is syntactically correct
     *
     * @param schema  that can be shacl (ttl), vocabulary(SKOS), ontology(owl), and needs to be verified
     * @return          TRUE if the schema is syntactically valid
     */
    boolean verifySchema(MultipartFile schema) ;
    /**
     * store a schema after has been successfully verified for its type and syntax
     *
     * @param schema  to be stored
     */
    void addSchema(MultipartFile schema);
}
