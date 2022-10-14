package eu.gaiax.difs.fc.core.service.verification.impl;

import apoc.coll.Coll;
import com.danubetech.verifiablecredentials.VerifiablePresentation;
import eu.gaiax.difs.fc.core.config.DatabaseConfig;
import eu.gaiax.difs.fc.core.config.FileStoreConfig;
import eu.gaiax.difs.fc.core.exception.VerificationException;
import eu.gaiax.difs.fc.core.pojo.*;
import eu.gaiax.difs.fc.core.service.filestore.FileStore;
import eu.gaiax.difs.fc.core.service.graphdb.impl.Neo4jGraphStore;
import eu.gaiax.difs.fc.core.service.schemastore.impl.SchemaStoreImpl;
import eu.gaiax.difs.fc.core.service.sdstore.SelfDescriptionStore;
import eu.gaiax.difs.fc.core.service.sdstore.impl.SelfDescriptionStoreImpl;
import eu.gaiax.difs.fc.core.service.sdstore.impl.SelfDescriptionStoreImplTest;
import eu.gaiax.difs.fc.core.service.validatorcache.ValidatorCache;
import eu.gaiax.difs.fc.core.service.validatorcache.impl.ValidatorCacheImpl;
import eu.gaiax.difs.fc.testsupport.config.EmbeddedNeo4JConfig;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.ejb.HibernateEntityManagerFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("tests-sdstore")
@ContextConfiguration(classes = {
        VerificationServiceImplTest.TestApplication.class,
        VerificationServiceImpl.class,
        SelfDescriptionStoreImplTest.class,
        SchemaStoreImpl.class,
        SelfDescriptionStoreImpl.class,
        Neo4jGraphStore.class,
        FileStoreConfig.class,
        DatabaseConfig.class})
@DirtiesContext
@Transactional
@AutoConfigureEmbeddedDatabase(provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY)
@Slf4j
@Import(EmbeddedNeo4JConfig.class)
public class VerificationServiceImplTest {

    static Path base_path = Paths.get(".").toAbsolutePath().normalize();

    @SpringBootApplication
    public static class TestApplication {

        public static void main(final String[] args) {
            SpringApplication.run(TestApplication.class, args);
        }
    }

    @Autowired
    private VerificationServiceImpl verificationService;

    @Autowired
    private SelfDescriptionStore selfDescriptionStore;

    @Autowired
    private Neo4jGraphStore graphStore;

    @Autowired
    @Qualifier("sdFileStore")
    private FileStore fileStore;

    private static ContentAccessor getAccessor(String path) throws UnsupportedEncodingException {
        URL url = VerificationServiceImplTest.class.getClassLoader().getResource(path);
        String str = URLDecoder.decode(url.getFile(), StandardCharsets.UTF_8.name());
        return new ContentAccessorFile(new File(str));
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
        assertEquals("Could not find a VC in SD", ex.getMessage());
    }

    @Test
    void invalidProof_SignatureHasInvalidType () throws IOException {
        String path = "VerificationService/sign/hasInvalidSignatureType.jsonld";

        Exception ex = assertThrowsExactly(VerificationException.class, () ->
                verificationService.verifySelfDescription(getAccessor(path)));
        System.out.println(ex.getMessage());
        assertEquals("SD is neither a Participant SD nor a ServiceOffer SD", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    void invalidProof_SignaturesMissing1() throws IOException {
        String path = "VerificationService/sign/hasNoSignature1.jsonld";

        Exception ex = assertThrowsExactly(VerificationException.class, () ->
                verificationService.verifySelfDescription(getAccessor(path)));
        System.out.println(ex.getMessage());
        assertEquals("No proof found", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    @Disabled("We need an SD with valid proofs")
    void invalidProof_SignaturesMissing2() throws IOException {
        String path = "VerificationService/sign/lacksSomeSignatures.jsonld";

        Exception ex = assertThrowsExactly(VerificationException.class, () ->
                verificationService.verifySelfDescription(getAccessor(path)));
        System.out.println(ex.getMessage());
        assertEquals("No proof found", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    @Disabled() //TODO
    void verifySignature_InvalidSignature () throws UnsupportedEncodingException {
        String path = "VerificationService/sign/hasInvalidSignature.jsonld";

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

    @Test
    @Disabled("This test wont work like this anymore since some functions are private now")
    void providerClaimsTest() throws Exception {
        String path = "Claims-Extraction-Tests/providerTest.jsonld";

        VerificationResult result = verificationService.verifySelfDescription(getAccessor(path));
        List<SdClaim> actualClaims = result.getClaims();

        List<SdClaim> expectedClaims = new ArrayList<>();
        expectedClaims.add(new SdClaim("_:b0", "<vcard:country-name>", "\"Country Name 2\""));
        expectedClaims.add(new SdClaim("_:b0", "<vcard:locality>", "\"City Name 2\""));
        expectedClaims.add(new SdClaim("_:b0", "<vcard:postal-code>", "\"99999\""));
        expectedClaims.add(new SdClaim("_:b0", "<vcard:street-address>", "\"Example street 2\""));
        expectedClaims.add(new SdClaim("<http://example.org/test-issuer>", "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>", "<gax:Provider>"));
        expectedClaims.add(new SdClaim("<http://example.org/test-issuer>", "<gax:hasLegallyBindingAddress>", "_:b0"));
        expectedClaims.add(new SdClaim("<http://example.org/test-issuer>", "<gax:hasLegallyBindingName>", "\"My example provider\""));

        assertTrue(expectedClaims.size() == actualClaims.size());
        assertTrue(expectedClaims.containsAll(actualClaims));
        assertTrue(actualClaims.containsAll(expectedClaims));
    }

    @Test
    void playingWithClaims() throws UnsupportedEncodingException {
        String path = "serviceOfferingSD.jsonld";


        ContentAccessor contentAccessor = getAccessor(path);
        VerificationResult verificationResult = verificationService.verifySelfDescription(contentAccessor);

        SelfDescriptionMetadata sdMetadata = new SelfDescriptionMetadata(verificationResult.getId(), verificationResult.getIssuer(),
                verificationResult.getValidators(), contentAccessor);

        selfDescriptionStore.storeSelfDescription(sdMetadata, verificationResult);

        String credentialSubject = "http://example.org/test-issuer2";  // == sdMetadata.getIssuer()
        System.out.println("########################## Issuer: " + sdMetadata.getIssuer());
        System.out.println("########################## ID: " + sdMetadata.getId());
        System.out.println("########################## SD: " + sdMetadata.getSelfDescription());

        ContentAccessor sdAccessor = sdMetadata.getSelfDescription();
        System.out.println("########################## SD content str: " + sdAccessor.getContentAsString());

        // get the data back from SD store
        SdFilter filter = new SdFilter();
        filter.setIssuers(Collections.singletonList(credentialSubject));
        List<SelfDescriptionMetadata> results = selfDescriptionStore.getByFilter(filter).getResults();
        System.out.println("########################## Number of results: " + results.size());

        SelfDescriptionMetadata firstResult = results.get(0);
        VerifiablePresentation vp =
                VerifiablePresentation.fromJson(firstResult.getSelfDescription().getContentAsString()
                        .replaceAll("JsonWebKey2020", "JsonWebSignature2020"));

        Map<String, Object> claims =
                vp.getVerifiableCredential().getCredentialSubject().getClaims();

        System.out.println("########################## claims: " + claims);
        fail("Planned failure");
    }

    @Test
    void verifyValidationResult() throws IOException {
        String dataPath = "Validation-Tests/DataCenterDataGraph.jsonld";
        String shapePath = "Validation-Tests/physical-resourceShape.ttl";
        SemanticValidationResult validationResult = verificationService.validationAgainstShacl(
                getAccessor(dataPath), getAccessor(shapePath));

        if (!validationResult.isConforming()) {
            assertTrue(validationResult.getValidationReport().contains("Property needs to have at least 1 value"));
        } else {
            assertFalse(validationResult.isConforming());
        }
    }
}