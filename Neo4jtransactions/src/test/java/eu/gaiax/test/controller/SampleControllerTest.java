package eu.gaiax.test.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

//import eu.gaiax.difs.fc.testsupport.config.EmbeddedNeo4JConfig;
//import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
//import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension.class)
//@AutoConfigureEmbeddedDatabase(provider = DatabaseProvider.ZONKY)
//@Import(EmbeddedNeo4JConfig.class)
public class SampleControllerTest {
    
    @Autowired
    private MockMvc mockMvc;


    @Test
    void testSample() throws Exception {
        
        mockMvc.perform(MockMvcRequestBuilders.post("/test"))
            .andExpect(status().isOk());
    }

}
