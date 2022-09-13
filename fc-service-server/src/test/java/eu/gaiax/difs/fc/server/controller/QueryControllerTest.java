package eu.gaiax.difs.fc.server.controller;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.gaiax.difs.fc.api.generated.model.Result;
import eu.gaiax.difs.fc.core.pojo.ContentAccessorDirect;
import eu.gaiax.difs.fc.core.pojo.SelfDescriptionMetadata;
import eu.gaiax.difs.fc.core.pojo.VerificationResultOffering;
import eu.gaiax.difs.fc.core.service.graphdb.impl.Neo4jGraphStore;
import eu.gaiax.difs.fc.core.service.sdstore.SelfDescriptionStore;
import eu.gaiax.difs.fc.core.service.verification.VerificationService;
import eu.gaiax.difs.fc.server.config.EmbeddedNeo4JConfig;
import eu.gaiax.difs.fc.server.helper.FileReaderHelper;
import eu.gaiax.difs.fc.server.service.SelfDescriptionService;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;
import java.io.IOException;
import java.util.ArrayList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.neo4j.harness.Neo4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.annotation.BeforeTestClass;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension.class)
@AutoConfigureEmbeddedDatabase(provider = DatabaseProvider.ZONKY)
@Import(EmbeddedNeo4JConfig.class)
public class QueryControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Neo4jGraphStore graphStore;

    @Autowired
    private Neo4j embeddedDatabaseServer;

    @Autowired
    private SelfDescriptionStore sdStore;

    @Autowired
    private SelfDescriptionService selfDescriptionService;

    @Autowired
    private VerificationService verificationService;

    @AfterAll
    void closeNeo4j() {
        embeddedDatabaseServer.close();
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final static String SD_FILE_NAME = "test-provider-sd.json";


    @BeforeTestClass
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    String QUERY_REQUEST_GET="{\"statement\": \"Match (m:Address) where m.postal-code > 99999 RETURN m\", " +
        "\"parameters\": " +
        "null}}";

    String QUERY_REQUEST_POST="{\"statement\": \"CREATE (m:Address {postal-code: '99999', address : 'test'})\", " +
        "\"parameters\": " +
        "null}}";

    String QUERY_REQUEST_UPDATE="{\"statement\": \"Match (m:Address) where m.postal-code > 99999 SET m.postal-code = " +
        "88888 RETURN m\", " +
        "\"parameters\": " +
        "null}}";

    String QUERY_REQUEST_DELETE="{\"statement\": \"Match (m:Address) where m.postal-code > 99999 DELETE m\", " +
        "\"parameters\": " +
        "null}}";
    @Test
    public void getQueryPageShouldReturnSuccessResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/query")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(header().stringValues("Content-Type", "text/html"));
    }

    @Test
    @Disabled("Enable when FH implementation is done")
    public void postQueriesReturnSuccessResponse() throws Exception {

        initialiseAllDataBaseWithManuallyAddingSDFromRepository();

        String response =  mockMvc.perform(MockMvcRequestBuilders.post("/query")
                        .content(QUERY_REQUEST_GET)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Produces","application/json","application/sparql-results+xml", "text/turtle", "text/html")
                        .header("Accept","application/json") //,"application/sparql-query","application/sparql*")
                        .header("query-language","application/sparql-query"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();


        Result result = objectMapper.readValue(response, Result.class);
        assertEquals(1, result.getData().size());
    }


    @Test
    @Disabled("Enable  when FH implementation is done")
    public void postQueryReturnForbiddenResponse() throws Exception {
          mockMvc.perform(MockMvcRequestBuilders.post("/query")
                .content(QUERY_REQUEST_POST)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Produces","application/json","application/sparql-results+xml", "text/turtle", "text/html")
                .header("Accept","application/json") //,"application/sparql-query","application/sparql*")
                .header("query-language","application/sparql-query"))
            .andExpect(status().isForbidden());

    }
    @Test
    @Disabled("Enable  when FH implementation is done")
    public void postQueryForUpdateReturnForbiddenResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/query")
                .content(QUERY_REQUEST_UPDATE)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Produces","application/json","application/sparql-results+xml", "text/turtle", "text/html")
                .header("Accept","application/json") //,"application/sparql-query","application/sparql*")
                .header("query-language","application/sparql-query"))
            .andExpect(status().isForbidden());

    }

    @Test
    @Disabled("Enable  when FH implementation is done")
    public void postQueryForDeleteReturnForbiddenResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/query")
                .content(QUERY_REQUEST_DELETE)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Produces","application/json","application/sparql-results+xml", "text/turtle", "text/html")
                .header("Accept","application/json") //,"application/sparql-query","application/sparql*")
                .header("query-language","application/sparql-query"))
            .andExpect(status().isForbidden());

    }

    private void initialiseAllDataBaseWithManuallyAddingSDFromRepository() throws IOException {
        ContentAccessorDirect contentAccessor = new ContentAccessorDirect(FileReaderHelper.getMockFileDataAsString(SD_FILE_NAME));
        VerificationResultOffering verificationResult = verificationService.verifyOfferingSelfDescription(contentAccessor);
        SelfDescriptionMetadata sdMetadata = new SelfDescriptionMetadata(contentAccessor,
            verificationResult.getId(), verificationResult.getIssuer(), new ArrayList<>());
        sdStore.storeSelfDescription(sdMetadata, verificationResult);

    }
}
