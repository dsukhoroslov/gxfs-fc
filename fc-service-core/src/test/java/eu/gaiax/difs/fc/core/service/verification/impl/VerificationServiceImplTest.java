package eu.gaiax.difs.fc.core.service.verification.impl;

import eu.gaiax.difs.fc.core.exception.VerificationException;
import eu.gaiax.difs.fc.core.service.sdstore.impl.ContentAccessorDirect;
import eu.gaiax.difs.fc.core.service.sdstore.impl.ContentAccessorFile;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class VerificationServiceImplTest {
    static Path base_path = Paths.get(".").toAbsolutePath().normalize();
    private final VerificationServiceImpl verificationService = new VerificationServiceImpl();

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
       // String data =  getAccessor(dataPath).getContentAsString();

        String shapePath = "Validation-Tests/physical-resourceShape.ttl";
        //String shape =  getAccessor(shapePath).getContentAsString();

        boolean actual = verificationService.isValidAgainstShacl(getAccessor(dataPath), getAccessor(shapePath));
        if(actual==false) {
            String outputReportPath = "Validation-Tests/report1.jsonld";
            ContentAccessorFile outputContentAccessor=  getAccessor(outputReportPath);
            Map<String, Object> cred = verificationService.parseSD(outputContentAccessor);
            ArrayList<Object> map = ( ArrayList<Object>)cred.get("@graph");
            String resultMessage = "Property needs to have at least 1 value";
            assertTrue(map.get(0).toString().contains(resultMessage));
        }else {
            assertFalse(actual);
        }

    }

}