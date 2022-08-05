package eu.gaiax.difs.fc.core.service.schemaManagement.impl;

import eu.gaiax.difs.fc.core.service.schemaManagement.SchemaManagementService;
import org.apache.commons.io.FilenameUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.shacl.Shapes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.web.multipart.MultipartFile;
import org.topbraid.shacl.validation.ValidationUtil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SchemaManagementServiceImpl implements SchemaManagementService {

    private static final Path BASE_PATH = Paths.get(".").toAbsolutePath().normalize();
    private static final String SHACL_SHACL =  "/src/test/resources/schema-management-tests/shacl-shacl.ttl";
    private static final String SHACL_DIR = BASE_PATH.toFile().getAbsolutePath() + "/src/test/resources/schema-management-tests/shacl/";
    private static final Marker WTF_MARKER = MarkerFactory.getMarker("WTF");
    private static final Logger logger = LoggerFactory.getLogger(SchemaManagementServiceImpl.class);
    /**
     * Retrieve the pre-defined shacl shapes files stored in the resources folder of the remote Repository
     *
     * @return         list of shacl shape files
     */

    public List<File> getShaclFiles() {
        String shaclFolderPathName = BASE_PATH.toFile().getAbsolutePath() + "/src/test/resources/Validation-Tests/shacl";
        List<File> shaclFilelistFiles = new ArrayList<>();
        File shaclFolder = new File(shaclFolderPathName);
        File[] list = shaclFolder.listFiles();
        for (File file: list) {
            if (!file.isDirectory()) {
                shaclFilelistFiles.add(file.getAbsoluteFile());
            }
        }
        return shaclFilelistFiles;
    }
    /**
     * Upload a local shacl files to the pre-defined shacl folder in the Remote repository
     *
     * @param shaclFile shacl file to be uploaded
     */

    public void uploadJSONLDShacl(MultipartFile shaclFile) {
        String data = SHACL_DIR+shaclFile.getOriginalFilename();
        try {
            if(isJSONLD(shaclFile) && verifySchema(shaclFile)){
                Files.copy(shaclFile.getInputStream(), Path.of(data));
            } else {
                throw new RuntimeException("is not jsonld");
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not store the file. Error: " + e.getMessage());

        }
    }
    /**
     * Upload a local shacl files to the pre-defined shacl folder in the Remote repository
     *
     * @param shaclFile shacl file to be uploaded
     */

    public void uploadXMLOrTurtlShacl(MultipartFile shaclFile) {
        String data = SHACL_DIR+shaclFile.getOriginalFilename();
        try {
            if(isXMLOrTurtl(shaclFile) && verifySchema(shaclFile)){
                Files.copy(shaclFile.getInputStream(), Path.of(data));
            } else if(isJSONLD(shaclFile)) {
                //TODO convert to turtle and upload the converted file
            } else {

                throw new RuntimeException("is not turtle or xml");
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not store the file. Error: " + e.getMessage());

        }
    }

    /**
     * check if a given shacl file has JASON-LD extension
     *
     * @param shaclFile shacl file to be verified for its extension
     * @return          TRUE if shaclFile is JSON-LD
     */
    public boolean isJSONLD(MultipartFile shaclFile) {

        String fileExtension = FilenameUtils.getExtension(shaclFile.getOriginalFilename());
        if(fileExtension.equals("jsonld")){
            return true;
        } else {
            return false;
        }

    }
    /**
     * check if a given shacl file has XML or Turtle extension and NOT
     * JASON-LD
     *
     * @param shaclFile shacl file to be verified for its extension
     * @return          TRUE if shaclFile is XML or Turtle
     */
    public boolean isXMLOrTurtl(MultipartFile shaclFile) {

        String fileExtension = FilenameUtils.getExtension(shaclFile.getOriginalFilename());
        if(!fileExtension.equals("jsonld") && (fileExtension.equals("xml") || fileExtension.equals("ttl"))){
            return true;
        } else {
            return false;
        }
    }

    /**
     * verify if a given schema is syntactically correct
     *
     * @param schemaFile  that can be shacl (ttl), vocabulary(SKOS), ontology(owl), and needs to be verified
     * @return          TRUE if the schema is syntactically valid
     */
    @Override
    public boolean verifySchema(MultipartFile schemaFile) {
        //TODO distinguish between specifications , and return results
        boolean flag = false;
        try {
            //String shapes = BASE_PATH.toFile().getAbsolutePath() + schema;
            String schema = new String(schemaFile.getBytes());
            Shapes myShape = Shapes.parse(schema);
            flag = true;

        } catch (Throwable t) {
            logger.error(WTF_MARKER, t.toString(), t);
        }
        return flag;

    }
    /**
     * store a schema after has been successfully verified for its type and syntax
     *
     * @param schema  to be stored
     */
    @Override
    public void addSchema(MultipartFile schema) {
        uploadJSONLDShacl(schema);
        uploadXMLOrTurtlShacl(schema);
    }
}
