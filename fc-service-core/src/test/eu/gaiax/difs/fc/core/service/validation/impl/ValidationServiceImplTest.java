package eu.gaiax.difs.fc.core.service.validation.impl;

import eu.gaiax.difs.fc.core.exception.ValidationException;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class ValidationServiceImplTest {
    static Path base_path = Paths.get(".").toAbsolutePath().normalize();
    private final ValidationServiceImpl validationService = new ValidationServiceImpl();
    private String readFile (String relPath) {
        String absPath = base_path.toFile().getAbsolutePath() + relPath;
        StringBuilder result = new StringBuilder();

        try {
            BufferedReader reader = new BufferedReader(new FileReader(absPath));
            String line = "";
            do {
                result.append(line);
                line = reader.readLine();
            } while (line != null);
        } catch (FileNotFoundException e) {
            System.out.println(absPath);
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result.toString();
    }

    @Test
    void verifyJSON_LD_valid_Example_1() {
        String path = "/src/test/resources/JSON-LD-Tests/valid1.jsonld";
        String json = readFile(path);

        assertDoesNotThrow(() -> {
            validationService.parseSD(json);
        });
    }

    @Test
    void verifyJSON_LD_valid_Example_2() {
        String path = "/src/test/resources/JSON-LD-Tests/valid2.jsonld";
        String json = readFile(path);

        assertDoesNotThrow(() -> {
            validationService.parseSD(json);
        });
    }

    @Test
    void verifyJSON_LD_invalid_Example_1() {
        String path = "/src/test/resources/JSON-LD-Tests/invalid1.jsonld";
        String json = readFile(path);

        assertThrowsExactly(ValidationException.class, () -> validationService.verifySelfDescription(json));
    }
}