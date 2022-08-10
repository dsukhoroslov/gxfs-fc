package eu.gaiax.difs.fc.core.service.validation.impl;

import eu.gaiax.difs.fc.core.exception.ValidationException;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ValidationServiceImplTest {
    static Path base_path = Paths.get(".").toAbsolutePath().normalize();
    private final ValidationServiceImpl validationService = new ValidationServiceImpl();
    private String readFile (String relPath) throws IOException {
        String absPath = base_path.toFile().getAbsolutePath() + relPath;
        StringBuilder result = new StringBuilder();

        BufferedReader reader = new BufferedReader(new FileReader(absPath));
        String line = "";
        do {
            result.append(line);
            line = reader.readLine();
        } while (line != null);

        return result.toString();
    }

    @Test
    void verifyJSON_LD_valid_Example_1() throws IOException {
        String path = "/src/test/resources/JSON-LD-Tests/valid1.jsonld";
        String json = readFile(path);

        assertDoesNotThrow(() -> {
            validationService.parseSD(json);
        });
    }

    @Test
    void verifyJSON_LD_valid_Example_2() throws IOException {
        String path = "/src/test/resources/JSON-LD-Tests/valid2.jsonld";
        String json = readFile(path);

        assertDoesNotThrow(() -> {
            validationService.parseSD(json);
        });
    }

    @Test
    void verifyJSON_LD_invalid_Example_1() throws IOException {
        String path = "/src/test/resources/JSON-LD-Tests/invalid1.jsonld";
        String json = readFile(path);

        assertThrowsExactly(ValidationException.class, () -> validationService.verifySelfDescription(json));
    }

    @Test
    void verifySignature_invalid1() throws IOException {
        String path = "/src/test/resources/Signature-Tests/hasInvalidSignature.jsonld";
        String json = readFile(path);

        Map<String, Object> parsed = validationService.parseSD (json);

        //TODO: Will throw exception when it is checked cryptographically
        assertDoesNotThrow(() -> validationService.validateCryptographic(parsed));
    }

    @Test
    void verifySignature_invalid2() throws IOException {
        String path = "/src/test/resources/Signature-Tests/hasNoSignature1.jsonld";
        String json = readFile(path);

        Map<String, Object> parsed = validationService.parseSD (json);

        assertThrowsExactly(ValidationException.class, () -> validationService.validateCryptographic(parsed));
    }

    @Test
    void verifySignature_invalid3() throws IOException {
        String path = "/src/test/resources/Signature-Tests/hasNoSignature2.jsonld";
        String json = readFile(path);

        Map<String, Object> parsed = validationService.parseSD (json);

        assertThrowsExactly(ValidationException.class, () -> validationService.validateCryptographic(parsed));
    }

    @Test
    void verifySignature_invalid4() throws IOException {
        String path = "/src/test/resources/Signature-Tests/lacksSomeSignatures.jsonld";
        String json = readFile(path);

        Map<String, Object> parsed = validationService.parseSD (json);

        assertThrowsExactly(ValidationException.class, () -> validationService.validateCryptographic(parsed));
    }

    @Test
    void verifySignature_cleanSD1() throws IOException {
        String path = "/src/test/resources/Signature-Tests/hasInvalidSignature.jsonld";
        String json = readFile(path);

        Map<String, Object> parsed = validationService.parseSD (json);

        //Do proofs exist?
        assertTrue(parsed.containsKey("proof"));
        for (Map<String, Object> credential : (ArrayList<Map<String, Object>>) parsed.get("verifiableCredential")) {
            assertTrue(credential.containsKey("proof"));
        }

        Map<String, Object> cleaned = validationService.cleanSD (parsed);

        //Are proofs removed?
        assertFalse(cleaned.containsKey("proof"));
        for (Map<String, Object> credential : (ArrayList<Map<String, Object>>) cleaned.get("verifiableCredential")) {
            assertFalse(credential.containsKey("proof"));
        }
    }
}