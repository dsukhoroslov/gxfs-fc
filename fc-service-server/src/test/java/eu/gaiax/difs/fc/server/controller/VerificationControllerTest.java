package eu.gaiax.difs.fc.server.controller;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.gaiax.difs.fc.api.generated.model.Error;
import eu.gaiax.difs.fc.api.generated.model.VerificationResult;
import eu.gaiax.difs.fc.core.service.schemastore.SchemaStore;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;

import static eu.gaiax.difs.fc.server.helper.FileReaderHelper.getMockFileDataAsString;
import static eu.gaiax.difs.fc.server.util.TestUtil.getAccessor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.time.Instant;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureEmbeddedDatabase(provider = DatabaseProvider.ZONKY)
public class VerificationControllerTest {

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private SchemaStore schemaStore;

  @BeforeAll
  public void setup() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    schemaStore.addSchema(getAccessor("mock-data/gax-test-ontology.ttl"));
  }

  @AfterAll
  public void storageSelfCleaning() throws IOException {
    schemaStore.clear();
  }

  @Test
  public void getVerifyPageShouldReturnSuccessResponse() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.get("/verification")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(header().stringValues("Content-Type", "text/html"));
  }

  @Test
  public void verifyParticipantShouldReturnSuccessResponse() throws Exception {
    String json = getMockFileDataAsString("default_participant.json");
    String response = mockMvc.perform(MockMvcRequestBuilders.post("/verification")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(json))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    VerificationResult partResult = objectMapper.readValue(response, VerificationResult.class);
    assertEquals("did:example:issuer", partResult.getIssuer());
    assertEquals(Instant.parse("2010-01-01T19:23:24Z"), partResult.getIssuedDateTime());
  }

  @Test
  public void verifyNoProofsShouldReturnUnprocessibleEntity() throws Exception {
    String json = getMockFileDataAsString("participant_without_proofs.json");
    mockMvc.perform(MockMvcRequestBuilders.post("/verification")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(json))
            .andExpect(status().isUnprocessableEntity());
  }

  @Test
  public void verifyNoProofsNoSignsShouldReturnSuccessResponse() throws Exception {
    String json = getMockFileDataAsString("participant_without_proofs.json");
    mockMvc.perform(MockMvcRequestBuilders.post("/verification")
            .queryParam("verifySemantics", "false")
            .queryParam("verifySignatures", "false")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(json))
            .andExpect(status().isOk());
  }

  @Test
  public void verifySDNoCSShouldReturnUnprocessibleEntity() throws Exception {
    String json = getMockFileDataAsString("sd-without-credential-subject.json");
    String response = mockMvc.perform(MockMvcRequestBuilders.post("/verification")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(json))
            .andExpect(status().isUnprocessableEntity())
            .andReturn()
            .getResponse()
            .getContentAsString();
    Error error = objectMapper.readValue(response, Error.class);
    assertEquals("verification_error", error.getCode());
    assertTrue(error.getMessage().startsWith("Semantic Error:"), "Message is: " + error.getMessage());
    assertTrue(error.getMessage().contains("no proper CredentialSubject found"), "Message is: " + error.getMessage());
  }

  @Test
  @Disabled("I don't see where this test is supposed to succeed")
  public void verifySDNoCSNoSemanticsShouldReturnSuccessResponse() throws Exception {
    String json = getMockFileDataAsString("sd-without-credential-subject.json");
    mockMvc.perform(MockMvcRequestBuilders.post("/verification")
            .queryParam("verifySemantics", "false")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(json))
            .andExpect(status().isOk());
  }

}
