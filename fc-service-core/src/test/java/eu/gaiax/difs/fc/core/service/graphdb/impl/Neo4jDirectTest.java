package eu.gaiax.difs.fc.core.service.graphdb.impl;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.FixMethodOrder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.runners.MethodSorters;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.harness.Neo4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import eu.gaiax.difs.fc.core.pojo.ContentAccessor;
import eu.gaiax.difs.fc.core.pojo.ContentAccessorFile;
import eu.gaiax.difs.fc.core.pojo.SdClaim;
import eu.gaiax.difs.fc.core.service.verification.impl.VerificationServiceImplTest;
import eu.gaiax.difs.fc.testsupport.config.EmbeddedNeo4JConfig;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@EnableAutoConfiguration(exclude = {LiquibaseAutoConfiguration.class, DataSourceAutoConfiguration.class})
@ActiveProfiles({"tests-sdstore"})
@ContextConfiguration(classes = {Neo4jDirectTest.class})
@Import(EmbeddedNeo4JConfig.class)
public class Neo4jDirectTest {

    // TODO: this test is to see how Neo4j works only, will be removed at some point later on
    
    @Autowired
    private Neo4j embeddedDatabaseServer;
    
    @Autowired
    private Driver driver;

    @AfterAll
    void closeNeo4j() {
        embeddedDatabaseServer.close();
    }
 
    
    @Test
    void testDirectLoadJsonLd() throws Exception {
        //String path = "Claims-Extraction-Tests/neo4jTest.jsonld"; 
        //String path = "Claims-Extraction-Tests/participantTest.jsonld"; 
        String path = "Claims-Extraction-Tests/providerTest.jsonld";
        ContentAccessor content = getAccessor(path);
        
        try (Session session = driver.session()) {
            String query = "CALL n10s.rdf.import.inline($payload, \"JSON-LD\")\n"
                    + "YIELD terminationStatus, triplesLoaded, triplesParsed, namespaces, extraInfo\n"
                    + "RETURN terminationStatus, triplesLoaded, triplesParsed, namespaces, extraInfo";
            
            //CALL n10s.rdf.import.fetch("file:///C:\Users\name\file_loaction\file_name.jsonld","JSON-LD");

            log.debug("addClaims; Query: {}", query);
            Result rs = session.run(query, Map.of("payload", content.getContentAsString()));
            log.debug("addClaims.exit; results: {}", rs.list());
        }
    }
    
    
    private static ContentAccessor getAccessor(String path) throws UnsupportedEncodingException {
        URL url = VerificationServiceImplTest.class.getClassLoader().getResource(path);
        String str = URLDecoder.decode(url.getFile(), StandardCharsets.UTF_8.name());
        File file = new File(str);
        ContentAccessor accessor = new ContentAccessorFile(file);
        return accessor;
    }
    
}
