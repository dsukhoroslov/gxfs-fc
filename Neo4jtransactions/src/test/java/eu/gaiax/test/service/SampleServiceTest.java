package eu.gaiax.test.service;

import eu.gaiax.test.repository.AddressRepositoryPostgres;
import eu.gaiax.test.repository.PersonRepository;
import eu.gaiax.test.service.SampleService;
//import eu.gaiax.difs.fc.testsupport.config.EmbeddedNeo4JConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
//import org.neo4j.harness.Neo4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@Slf4j
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnableAutoConfiguration(exclude = {/*LiquibaseAutoConfiguration.class,*/ DataSourceAutoConfiguration.class})
@ContextConfiguration(classes = {SampleServiceTest.class, PersonRepository.class,
    AddressRepositoryPostgres.class, SampleService.class})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
//@Import(EmbeddedNeo4JConfig.class)
public class SampleServiceTest {


  @Autowired
  private SampleService sampleService;

  //@Autowired
  //private Neo4j embeddedDatabaseServer;

  @Autowired
  private Neo4jClient neo4jClient;

  //@AfterAll
  //void closeNeo4j() {
  //  embeddedDatabaseServer.close();
  //}

  @Test
  //@Order(20)
  void testAddParticipantFailShouldRollbackGraphDbData() throws RuntimeException{

    //add particpant fails it should rollback graphdb Data
    Assertions.assertThrows(RuntimeException.class, () -> {
      sampleService.testTemplate();
    });

  }

  //@Test
  //@Order(10)
  void testParticipantFailShouldRollbackGraphDbDataFail() {

    Long nodeCount = neo4jClient.getQueryRunner().run("match (n) return n").stream().count();
    Assertions.assertEquals(1, nodeCount);
  }

}
