package eu.gaiax.difs.fc.core.service.verification.impl;

import eu.gaiax.difs.fc.core.exception.VerificationException;
import eu.gaiax.difs.fc.core.pojo.ContentAccessorFile;
import eu.gaiax.difs.fc.core.util.FileUtils;
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


    @Test
    void verifyJSONLDSyntax_valid1() {
        String path = "JSON-LD-Tests/validSD.jsonld";

        assertDoesNotThrow(() -> {
            verificationService.parseSD(FileUtils.getAccessorByPath(path));
        });
    }

    @Test
    void verifyJSONLDSyntax_valid2() {
        String path = "JSON-LD-Tests/smallExample.jsonld";

        assertDoesNotThrow(() -> {
            verificationService.parseSD(FileUtils.getAccessorByPath(path));
        });
    }

    @Test
    void verifyJSONLDSyntax_MissingQuote() {
        String path = "JSON-LD-Tests/missingQuote.jsonld";

        Exception ex = assertThrowsExactly(VerificationException.class, () -> verificationService.verifyOfferingSelfDescription(FileUtils.getAccessorByPath(path)));
        assertNotEquals("", ex.getMessage());
    }

    @Test
    @Disabled("The test is disabled because the check to throw the exception is not yet implemented")
    void verifySignature_SignatureDoesNotMatch() throws IOException {
        String path = "Signature-Tests/hasInvalidSignature.jsonld";

        Map<String, Object> parsed = verificationService.parseSD (FileUtils.getAccessorByPath(path));

        //TODO: Will throw exception when it is checked cryptographically
        assertThrowsExactly(VerificationException.class, () -> verificationService.validateCryptographic(parsed));
    }

    @Test
    void verifySignature_SignaturesMissing1() throws IOException {
        String path = "Signature-Tests/hasNoSignature1.jsonld";

        Map<String, Object> parsed = verificationService.parseSD (FileUtils.getAccessorByPath(path));

        Exception ex = assertThrowsExactly(VerificationException.class, () -> verificationService.validateCryptographic(parsed));
        assertEquals("no proof found", ex.getMessage());
    }

    @Test
    void verifySignature_SignaturesMissing2() throws IOException {
        String path = "Signature-Tests/hasNoSignature2.jsonld";

        Map<String, Object> parsed = verificationService.parseSD (FileUtils.getAccessorByPath(path));

        Exception ex = assertThrowsExactly(VerificationException.class, () -> verificationService.validateCryptographic(parsed));
        assertEquals("no proof found", ex.getMessage());
    }

    @Test
    void verifySignature_SignaturesMissing3() throws IOException {
        String path = "Signature-Tests/lacksSomeSignatures.jsonld";

        Map<String, Object> parsed = verificationService.parseSD (FileUtils.getAccessorByPath(path));

        Exception ex = assertThrowsExactly(VerificationException.class, () -> verificationService.validateCryptographic(parsed));
        assertEquals("no proof found", ex.getMessage());
    }

    @Test
    void cleanSD_removeProofs() throws IOException {
        String path = "Signature-Tests/hasInvalidSignature.jsonld";

        Map<String, Object> parsed = verificationService.parseSD (FileUtils.getAccessorByPath(path));

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
    void sdClaimsTest() throws IOException {
        String path = "Claims-Extraction-Tests/claimsTestsValid.jsonld";

        Map<String, Object> parsed = verificationService.parseSD (FileUtils.getAccessorByPath(path));
        String expected = "[(https://delta-dao.com/.well-known/serviceMVGPortal.json ,gax-service:providedBy ,https://delta-dao.com/.well-known/participant.json), " +
                "(https://delta-dao.com/.well-known/serviceMVGPortal.json ,gax-service:name ,EuProGigant Portal), (https://delta-dao.com/.well-known/serviceMVGPortal.json ,gax-service:description ,EuProGigant Minimal Viable Gaia-X Portal), " +
                "(https://delta-dao.com/.well-known/serviceMVGPortal.json ,gax-service:TermsAndConditions ,https://euprogigant.com/en/terms/), " +
                "(https://delta-dao.com/.well-known/serviceMVGPortal.json ,gax-service:TermsAndConditions ,contentHash)]";
        String actual = verificationService.extractClaims(parsed).toString();
        assertEquals(expected,actual);

    }

}