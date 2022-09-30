package eu.gaiax.difs.fc.core.service.verification.impl;

import com.danubetech.verifiablecredentials.VerifiableCredential;
import com.danubetech.verifiablecredentials.VerifiablePresentation;
import eu.gaiax.difs.fc.core.exception.VerificationException;
import eu.gaiax.difs.fc.core.pojo.ContentAccessorFile;
import foundation.identity.jsonld.JsonLDException;
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
import java.security.GeneralSecurityException;
import java.text.ParseException;
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

    //Syntax Validation
    @Test
    void verifyJSONLDSyntax_valid1() {
        //TODO use ContentAccessorFile
        String path = "VerificationService/syntax/validSD.jsonld";

        assertDoesNotThrow(() -> {
            verificationService.parseSD(getAccessor(path));
        });
    }

    @Test
    void verifyJSONLDSyntax_valid2() {
        String path = "VerificationService/syntax/smallExample.jsonld";

        assertDoesNotThrow(() -> {
            verificationService.parseSD(getAccessor(path));
        });
    }

    @Test
    void verifyJSONLDSyntax_MissingQuote() {
        String path = "VerificationService/syntax/missingQuote.jsonld";

        Exception ex = assertThrowsExactly(VerificationException.class, () -> verificationService.parseSD(getAccessor(path)));
        assertNotEquals("", ex.getMessage());
        assertNotNull(ex.getCause());
    }

    @Test
    void detectParticipantType() throws UnsupportedEncodingException {
        String path = "VerificationService/sd_type/participantA1digital.jsonld";
        VerifiablePresentation presentation = verificationService.parseSD(getAccessor(path));

        assertDoesNotThrow(() -> {
            verificationService.isSDParticipant(presentation);
        });

        assertTrue(verificationService.isSDParticipant(presentation));
        assertFalse(verificationService.isSDServiceOffering(presentation));
    }

    @Test
    void detectServiceOfferingType() throws UnsupportedEncodingException {
        String path = "VerificationService/sd_type/serviceAccessController.jsonld";
        VerifiablePresentation presentation = verificationService.parseSD(getAccessor(path));

        assertDoesNotThrow(() -> {
            verificationService.isSDParticipant(presentation);
        });

        assertFalse(verificationService.isSDParticipant(presentation));
        assertTrue(verificationService.isSDServiceOffering(presentation));

    }

    @Test
    void detectNoType() throws UnsupportedEncodingException {
        String path = "VerificationService/sd_type/smallExample.jsonld";
        VerifiablePresentation presentation = verificationService.parseSD(getAccessor(path));

        assertDoesNotThrow(() -> {
            verificationService.isSDParticipant(presentation);
        });

        assertFalse(verificationService.isSDParticipant(presentation));
        assertFalse(verificationService.isSDServiceOffering(presentation));
    }

    @Test
    void verifyValidPEM () {
        assertDoesNotThrow(() -> {
            verificationService.hasPEMTrustAnchorAndIsNotDeprecated("https://compliance.gaia-x.eu/.well-known/x509CertificateChain.pem");
        });
    }

    @Test
    void verifySignature_SignatureHasInvalidType() throws IOException {
        String path = "VerificationService/signature/hasInvalidSignatureType.jsonld";

        VerifiablePresentation parsed = verificationService.parseSD (getAccessor(path));

        //TODO: Will throw exception when it is checked cryptographically
        Exception ex = assertThrowsExactly(VerificationException.class, () -> verificationService.checkCryptographic(parsed));
    }

    @Test
    void verifySignature_SignaturesMissing1() throws IOException {
        String path = "VerificationService/signature/hasNoSignature1.jsonld";

        VerifiablePresentation presentation = verificationService.parseSD (getAccessor(path));

        Exception ex = assertThrowsExactly(VerificationException.class, () -> verificationService.checkCryptographic(presentation));
        assertEquals("No proof found", ex.getMessage());
    }

    @Test
    void verifySignature_SignaturesMissing2() throws IOException {
        String path = "VerificationService/signature/hasNoSignature2.jsonld";

        VerifiablePresentation presentation = verificationService.parseSD (getAccessor(path));

        Exception ex = assertThrowsExactly(VerificationException.class, () -> verificationService.checkCryptographic(presentation));
        assertEquals("No proof found", ex.getMessage());
    }

    @Test
    void verifySignature_SignaturesMissing3() throws IOException {
        String path = "VerificationService/signature/lacksSomeSignatures.jsonld";

        VerifiablePresentation presentation = verificationService.parseSD (getAccessor(path));

        Exception ex = assertThrowsExactly(VerificationException.class, () -> verificationService.checkCryptographic(presentation));
    }

    @Test
    void verifySignature_InvalidSignature () throws UnsupportedEncodingException {
        String path = "VerificationService/signature/hasInvalidSignature.jsonld";

        VerifiablePresentation presentation = verificationService.parseSD (getAccessor(path));

        assertThrowsExactly(VerificationException.class, () -> verificationService.checkCryptographic(presentation));
    }

    @Test
    @Disabled("We have no access to a correctly signed SD")
    void verifySignature_ValidSignature () throws UnsupportedEncodingException {
        String path = "VerificationService/signature/validSignature.jsonld";

        VerifiablePresentation presentation = verificationService.parseSD (getAccessor(path));

        assertThrowsExactly(VerificationException.class, () -> verificationService.checkCryptographic(presentation));

    }

    @Test
    void cleanSD_removeProofs() throws IOException {
        String path = "VerificationService/signature/hasInvalidSignatureType.jsonld";

        VerifiablePresentation presentation = verificationService.parseSD (getAccessor(path));

        //Do proof exist?
        assertNotNull(presentation.getLdProof());

        Map<String, Object> cleaned = verificationService.cleanSD (presentation);

        //Are proof removed?
        assertNull(presentation.getLdProof());
        assertNull(presentation.getJsonObject().get("proof"));
    }

    @Test
    @Disabled()
    void verifySignature () throws IOException, JsonLDException, GeneralSecurityException, ParseException {
        String path = "Claims-Extraction-Tests/participant-sd.json";

        VerifiableCredential credential = VerifiableCredential.fromJson(getAccessor(path).getContentAsString());
        verificationService.checkSignature(credential);
    }


}