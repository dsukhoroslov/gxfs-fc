package eu.gaiax.difs.fc.core.service.verification.impl;

import eu.gaiax.difs.fc.core.config.DatabaseConfig;
import eu.gaiax.difs.fc.core.config.EmbeddedNeo4JConfig;
import eu.gaiax.difs.fc.core.config.FileStoreConfig;
import eu.gaiax.difs.fc.core.exception.VerificationException;
import eu.gaiax.difs.fc.core.pojo.ContentAccessorFile;
import eu.gaiax.difs.fc.core.service.graphdb.impl.Neo4jGraphStore;
import eu.gaiax.difs.fc.core.service.schemastore.impl.SchemaStoreImpl;
import eu.gaiax.difs.fc.core.service.sdstore.SelfDescriptionStore;
import eu.gaiax.difs.fc.core.service.sdstore.impl.SelfDescriptionStoreImpl;
import eu.gaiax.difs.fc.core.service.sdstore.impl.SelfDescriptionStoreImplTest;
import eu.gaiax.difs.fc.core.service.verification.VerificationService;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.MethodName.class)
//@EnableAutoConfiguration(exclude = {LiquibaseAutoConfiguration.class, DataSourceAutoConfiguration.class, Neo4jTestHarnessAutoConfiguration.class})
@SpringBootTest
@ActiveProfiles("tests-sdstore")
@ContextConfiguration(classes = {VerificationServiceImplTest.TestApplication.class, FileStoreConfig.class,
        VerificationServiceImpl.class, VerificationServiceImplTest.class, DatabaseConfig.class, Neo4jGraphStore.class, SchemaStoreImpl.class})
@DirtiesContext
@Transactional
@Slf4j
@AutoConfigureEmbeddedDatabase(provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY)
@Import(EmbeddedNeo4JConfig.class)
public class VerificationServiceImplTest {
    @SpringBootApplication
    public static class TestApplication {

        public static void main(final String[] args) {
            SpringApplication.run(SelfDescriptionStoreImplTest.TestApplication.class, args);
        }
    }

    @Autowired
    private VerificationServiceImpl verificationService;
    private static ContentAccessorFile getAccessor(String path) throws UnsupportedEncodingException {
        URL url = VerificationServiceImplTest.class.getClassLoader().getResource(path);
        String str = URLDecoder.decode(url.getFile(), StandardCharsets.UTF_8.name());
        File file = new File(str);
        ContentAccessorFile accessor = new ContentAccessorFile(file);
        return accessor;
    }

    @Test
    void verifyJSONLDSyntax_valid1() throws IOException {
        //TODO use ContentAccessorFile
        String path = "JSON-LD-Tests/validSD.jsonld";

        assertDoesNotThrow(() -> {
            verificationService.parseSD(getAccessor(path));
        });
    }

    @Test
    void verifyJSONLDSyntax_valid2() throws IOException {
        String path = "JSON-LD-Tests/smallExample.jsonld";

        assertDoesNotThrow(() -> {
            verificationService.parseSD(getAccessor(path));
        });
    }

    @Test
    void verifyJSONLDSyntax_MissingQuote() throws IOException {
        String path = "JSON-LD-Tests/missingQuote.jsonld";

        Exception ex = assertThrowsExactly(VerificationException.class, () -> verificationService.verifyOfferingSelfDescription(getAccessor(path)));
        assertNotEquals("", ex.getMessage());
    }

    @Test
    @Disabled("The test is disabled because the check to throw the exception is not yet implemented")
    void verifySignature_SignatureDoesNotMatch() throws IOException {
        String path = "Signature-Tests/hasInvalidSignature.jsonld";

        Map<String, Object> parsed = verificationService.parseSD (getAccessor(path));

        //TODO: Will throw exception when it is checked cryptographically
        assertThrowsExactly(VerificationException.class, () -> verificationService.validateCryptographic(parsed));
    }

    @Test
    void verifySignature_SignaturesMissing1() throws IOException {
        String path = "Signature-Tests/hasNoSignature1.jsonld";

        Map<String, Object> parsed = verificationService.parseSD (getAccessor(path));

        Exception ex = assertThrowsExactly(VerificationException.class, () -> verificationService.validateCryptographic(parsed));
        assertEquals("no proof found", ex.getMessage());
    }

    @Test
    void verifySignature_SignaturesMissing2() throws IOException {
        String path = "Signature-Tests/hasNoSignature2.jsonld";

        Map<String, Object> parsed = verificationService.parseSD (getAccessor(path));

        Exception ex = assertThrowsExactly(VerificationException.class, () -> verificationService.validateCryptographic(parsed));
        assertEquals("no proof found", ex.getMessage());
    }

    @Test
    void verifySignature_SignaturesMissing3() throws IOException {
        String path = "Signature-Tests/lacksSomeSignatures.jsonld";

        Map<String, Object> parsed = verificationService.parseSD (getAccessor(path));

        Exception ex = assertThrowsExactly(VerificationException.class, () -> verificationService.validateCryptographic(parsed));
        assertEquals("no proof found", ex.getMessage());
    }

    @Test
    void cleanSD_removeProofs() throws IOException {
        String path = "Signature-Tests/hasInvalidSignature.jsonld";

        Map<String, Object> parsed = verificationService.parseSD (getAccessor(path));

        //Do proofs exist?
        assertTrue(parsed.containsKey("proof"));
        for (Map<String, Object> credential : (ArrayList<Map<String, Object>>) parsed.get("verifiableCredential")) {
            assertTrue(credential.containsKey("proof"));
        }

        Map<String, Object> cleaned = verificationService.cleanSD (parsed);

        //Are proofs removed?
        assertFalse(cleaned.containsKey("proof"));
        for (Map<String, Object> credential : (ArrayList<Map<String, Object>>) cleaned.get("verifiableCredential")) {
            assertFalse(credential.containsKey("proof"));
        }
    }

    @Test
    void verifyValidationResult() throws IOException {
        String dataPath = "Validation-Tests/DataCenterDataGraph.jsonld";
        String shapePath = "Validation-Tests/physical-resourceShape.ttl";
        boolean validationResult = verificationService.
                validationAgainstShacl(getAccessor(dataPath),
                        getAccessor(shapePath)).isConforming();
        if(validationResult==false) {
            String resultMessage = "Property needs to have at least 1 value";
            assertTrue(verificationService.validationAgainstShacl(getAccessor(dataPath),getAccessor(shapePath)).getValidationReport().contains(resultMessage));
        }else {
            assertFalse(validationResult);
        }

    }

}