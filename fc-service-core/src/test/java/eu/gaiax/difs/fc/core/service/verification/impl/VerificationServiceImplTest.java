package eu.gaiax.difs.fc.core.service.verification.impl;

import eu.gaiax.difs.fc.core.exception.VerificationException;
import eu.gaiax.difs.fc.core.pojo.ContentAccessorFile;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

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
    void invalidSyntax_MissingQuote () {
        String path = "VerificationService/syntax/missingQuote.jsonld";

        Exception ex = assertThrowsExactly(VerificationException.class, () ->
                verificationService.verifySelfDescription(getAccessor(path)));
        assertEquals("Parsing of SD failed", ex.getMessage());
        assertNotNull(ex.getCause());
    }

    @Test
    void invalidSyntax_IsNoSD () {
        String path = "VerificationService/syntax/smallExample.jsonld";

        Exception ex = assertThrowsExactly(VerificationException.class, () ->
                verificationService.verifySelfDescription(getAccessor(path)));
        assertEquals("Could not extract SD's type", ex.getMessage());
        assertNotNull(ex.getCause());
    }

    @Test
    void invalidSyntax_SignatureHasInvalidType () throws IOException {
        String path = "VerificationService/signature/hasInvalidSignatureType.jsonld";

        Exception ex = assertThrowsExactly(VerificationException.class, () ->
                verificationService.verifySelfDescription(getAccessor(path)));
        System.out.println(ex.getMessage());
        assertEquals("Could not extract SD's type", ex.getMessage());
        assertNotNull(ex.getCause());
    }

    @Test
    void invalidProof_SignaturesMissing1() throws IOException {
        String path = "VerificationService/signature/hasNoSignature1.jsonld";

        Exception ex = assertThrowsExactly(VerificationException.class, () ->
                verificationService.verifySelfDescription(getAccessor(path)));
        System.out.println(ex.getMessage());
        assertEquals("No proof found", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    @Disabled("We need an SD with valid proofs")
    void invalidProof_SignaturesMissing2() throws IOException {
        String path = "VerificationService/signature/lacksSomeSignatures.jsonld";

        Exception ex = assertThrowsExactly(VerificationException.class, () ->
                verificationService.verifySelfDescription(getAccessor(path)));
        System.out.println(ex.getMessage());
        assertEquals("No proof found", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    //@Disabled() //TODO
    void verifySignature_InvalidSignature () throws UnsupportedEncodingException {
        String path = "VerificationService/signature/hasInvalidSignature.jsonld";

        Exception ex = assertThrowsExactly(VerificationException.class, () ->
                verificationService.verifySelfDescription(getAccessor(path)));
        System.out.println(ex.getMessage());
        assertTrue(ex.getMessage().contains("does not match with proof"));
    }

    @Test
    @Disabled("We have no valid SD yet") //TODO
    void validSD () {
        String path = "VerificationService/validExample.jsonld";

        assertDoesNotThrow(() ->
                verificationService.verifySelfDescription(getAccessor(path)));
    }
}