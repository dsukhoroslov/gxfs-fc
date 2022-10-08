package eu.gaiax.difs.fc.core.service.verification.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.danubetech.verifiablecredentials.VerifiablePresentation;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;

import eu.gaiax.difs.fc.core.exception.VerificationException;
import eu.gaiax.difs.fc.core.pojo.ContentAccessor;
import eu.gaiax.difs.fc.core.pojo.ContentAccessorFile;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VerificationDirectTest {
    
    // TODO: this test is to see how Neo4j works only, will be removed at some point later on
    
    @Test
    void parseJSONLDDirectly() throws Exception {
        //String path = "Claims-Extraction-Tests/participantTest.jsonld";
        String path = "Claims-Extraction-Tests/neo4jTest.jsonld";
        //String path = "Claims-Extraction-Tests/providerTest.jsonld";
        ContentAccessor content = getAccessor(path);

        // Read the file into an Object (The type of this object will be a List, Map, String, Boolean,
        // Number or null depending on the root object in the file).
        Object jsonObject = JsonUtils.fromInputStream(content.getContentAsStream());
        // Create a context JSON map containing prefixes and definitions
        Map context = new HashMap();
        // Customise context...
        // Create an instance of JsonLdOptions with the standard JSON-LD options
        JsonLdOptions options = new JsonLdOptions();
        options.setProcessingMode(JsonLdOptions.JSON_LD_1_1);
        // Customise options...
        // Call whichever JSONLD function you want! (e.g. compact)
        //Object compact = JsonLdProcessor.compact(jsonObject, context, options);
        Object rdf = JsonLdProcessor.toRDF(jsonObject);
        // Print out the result (or don't, it's your call!)
        log.debug("RDF: {}; {}", rdf, JsonUtils.toPrettyString(rdf));
    }

    @Test
    void extractClaimsDirectly() throws Exception {
        ContentAccessor content = getAccessor("Claims-Extraction-Tests/providerTest.jsonld");
        VerifiablePresentation vp = VerifiablePresentation.fromJson(content.getContentAsString()
                  .replaceAll("JsonWebKey2020", "JsonWebSignature2020"));
        Map<String, Object> claims = vp.getVerifiableCredential().getCredentialSubject().getClaims();
        log.debug("provider claims: {}", claims);
        log.debug("provider RDF: {}", vp.getVerifiableCredential().getCredentialSubject().toDataset().toList());

        content = getAccessor("Claims-Extraction-Tests/participantTest.jsonld");
        vp = VerifiablePresentation.fromJson(content.getContentAsString()
                  .replaceAll("JsonWebKey2020", "JsonWebSignature2020"));
        claims = vp.getVerifiableCredential().getCredentialSubject().getClaims();
        log.debug("participant claims: {}", claims);
        log.debug("participant RDF: {}", vp.getVerifiableCredential().getCredentialSubject().toDataset().toList());

        //content = getAccessor("Claims-Extraction-Tests/participantSD.jsonld");
        //vp = VerifiablePresentation.fromJson(content.getContentAsString()
        //          .replaceAll("JsonWebKey2020", "JsonWebSignature2020"));
        //claims = vp.getVerifiableCredential().getCredentialSubject().getClaims();
        //log.debug("big participant claims: {}", claims);
        //log.debug("big participant RDF: {}", vp.getVerifiableCredential().getCredentialSubject().toDataset().toList());
    }
    
    private static ContentAccessor getAccessor(String path) throws UnsupportedEncodingException {
        URL url = VerificationServiceImplTest.class.getClassLoader().getResource(path);
        String str = URLDecoder.decode(url.getFile(), StandardCharsets.UTF_8.name());
        File file = new File(str);
        ContentAccessor accessor = new ContentAccessorFile(file);
        return accessor;
    }
    
}