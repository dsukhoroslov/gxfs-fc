package eu.gaiax.difs.fc.core.service.verification.impl;

import static eu.gaiax.difs.fc.core.util.TestUtil.getAccessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdEmbed;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.rdf.RdfDataset;
import com.apicatalog.rdf.RdfGraph;
import com.apicatalog.rdf.RdfTriple;
import com.danubetech.verifiablecredentials.VerifiableCredential;
import com.danubetech.verifiablecredentials.VerifiablePresentation;
import com.danubetech.verifiablecredentials.validation.Validation;
import com.github.jsonldjava.core.JsonLdConsts.Embed;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;

import eu.gaiax.difs.fc.core.pojo.ContentAccessor;
import eu.gaiax.difs.fc.core.pojo.SdClaim;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VerificationDirectTest {
    
    // TODO: this test is to see how Neo4j works only, will be removed at some point later on
    
    @Test
    void parseJSONLDDirectly() throws Exception {
        //String path = "Claims-Extraction-Tests/participantTest.jsonld";
        String path = "Claims-Extraction-Tests/neo4jTest.jsonld";
        //String path = "Claims-Extraction-Tests/providerTest.jsonld";
        ContentAccessor content = getAccessor(VerificationDirectTest.class, path);

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
    @Disabled()
    void parseJSONLDDirectly3() throws Exception {
        String path = "Claims-Extraction-Tests/providerTest.jsonld";
        ContentAccessor content = getAccessor(VerificationDirectTest.class, path);
        Object jsonObject = JsonUtils.fromInputStream(content.getContentAsStream());
        Object rdf = JsonLdProcessor.toRDF(jsonObject, new LoggingTripleCallback());
        log.debug("RDF: {}; {}", rdf, JsonUtils.toPrettyString(rdf));
    }
    
    @Test
    void extractClaimsDirectly() throws Exception {
        ContentAccessor content = getAccessor(VerificationDirectTest.class, "Claims-Extraction-Tests/providerTest.jsonld");
        VerifiablePresentation vp = VerifiablePresentation.fromJson(content.getContentAsString());
        Map<String, Object> claims = vp.getVerifiableCredential().getCredentialSubject().getClaims();
        log.debug("provider claims: {}", claims);
        log.debug("provider RDF: {}", vp.getVerifiableCredential().getCredentialSubject().toDataset().toList());

        content = getAccessor(VerificationDirectTest.class, "Claims-Extraction-Tests/participantTest.jsonld");
        vp = VerifiablePresentation.fromJson(content.getContentAsString());
        claims = vp.getVerifiableCredential().getCredentialSubject().getClaims();
        log.debug("participant claims: {}", claims);
        log.debug("participant RDF: {}", vp.getVerifiableCredential().getCredentialSubject().toDataset().toList());

        //content = getAccessor("Claims-Extraction-Tests/participantSD.jsonld");
        //vp = VerifiablePresentation.fromJson(content.getContentAsString());
        //claims = vp.getVerifiableCredential().getCredentialSubject().getClaims();
        //log.debug("big participant claims: {}", claims);
        //log.debug("big participant RDF: {}", vp.getVerifiableCredential().getCredentialSubject().toDataset().toList());
    }
    
    @Test
    void validateVP() throws Exception {
        //validate(VerifiableCredential verifiableCredential)
        ContentAccessor content = getAccessor(VerificationDirectTest.class, "VerificationService/jsonld/input.vp.jsonld");
        VerifiablePresentation vp = VerifiablePresentation.fromJson(content.getContentAsString());
        try {
            Validation.validate(vp);
            Assertions.assertNotNull(vp.getVerifiableCredential());
            Validation.validate(vp.getVerifiableCredential());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Test
    void testExtract() throws Exception {
        ContentAccessor content = getAccessor(VerificationDirectTest.class, "Claims-Extraction-Tests/providerTest.jsonld");
        Document doc = JsonDocument.of(content.getContentAsStream());
        com.apicatalog.jsonld.JsonLdOptions opts = new com.apicatalog.jsonld.JsonLdOptions();
        opts.setEmbed(JsonLdEmbed.ALWAYS);
        JsonArray arr = JsonLd.expand(doc).options(opts).get();
        log.debug("extractClaims; expanded: {}", arr);
        JsonObject vp = arr.get(0).asJsonObject();
        JsonArray vcs = vp.get("https://www.w3.org/2018/credentials#verifiableCredential").asJsonArray();
        JsonObject vc = vcs.get(0).asJsonObject();
        JsonArray graph = vc.get("@graph").asJsonArray();
        log.debug("extractClaims; graph: {}", graph);
        
        for (JsonValue val: graph) {
            JsonObject obj = val.asJsonObject();
            JsonArray css = obj.getJsonArray("https://www.w3.org/2018/credentials#credentialSubject");
            for (JsonValue cs: css) {
                Document csDoc = JsonDocument.of(cs.asJsonObject());
                RdfDataset rdf = JsonLd.toRdf(csDoc).produceGeneralizedRdf(true).get();
                RdfGraph rdfGraph = rdf.getDefaultGraph();
                List<RdfTriple> triples = rdfGraph.toList();
                for (RdfTriple triple: triples) {
                    log.debug("extractClaims; got triple: {}", triple);
                }
            }
        }
        
    }
    
    @Test
    void testFlatten() throws Exception {
        ContentAccessor content = getAccessor(VerificationDirectTest.class, "Claims-Extraction-Tests/providerTest.jsonld");
        Document doc = JsonDocument.of(content.getContentAsStream());
        JsonStructure str = JsonLd.flatten(doc).get();
        log.debug("extractClaims; structure: {}", str);
        /*
        JsonObject vp = arr.get(0).asJsonObject();
        JsonArray vcs = vp.get("https://www.w3.org/2018/credentials#verifiableCredential").asJsonArray();
        JsonObject vc = vcs.get(0).asJsonObject();
        JsonArray graph = vc.get("@graph").asJsonArray();
        for (JsonValue val: graph) {
            JsonObject obj = val.asJsonObject();
            JsonArray css = obj.getJsonArray("https://www.w3.org/2018/credentials#credentialSubject");
            for (JsonValue cs: css) {
                Document csDoc = JsonDocument.of(cs.asJsonObject());
                RdfDataset rdf = JsonLd.toRdf(csDoc).produceGeneralizedRdf(true).get();
                RdfGraph rdfGraph = rdf.getDefaultGraph();
                List<RdfTriple> triples = rdfGraph.toList();
                for (RdfTriple triple: triples) {
                    log.debug("extractClaims; got triple: {}", triple);
                }
            }
        }
        */
    }
    
    @Test
    void testJenaLoad() throws Exception {
        
    }

}
 