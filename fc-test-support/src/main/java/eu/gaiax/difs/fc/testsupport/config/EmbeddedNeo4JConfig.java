package eu.gaiax.difs.fc.testsupport.config;

import java.util.List;

import apoc.util.Utils;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.configuration.connectors.BoltConnector;
import org.neo4j.configuration.helpers.SocketAddress;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.springframework.boot.test.autoconfigure.Neo4jTestHarnessAutoConfiguration;
import org.neo4j.gds.catalog.GraphExistsProc;
import org.neo4j.gds.catalog.GraphListProc;
import org.neo4j.gds.catalog.GraphProjectProc;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import lombok.extern.slf4j.Slf4j;
import n10s.graphconfig.GraphConfigProcedures;
import n10s.rdf.load.RDFLoadProcedures;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConditionalOnProperty(value = "federated-catalogue.scope", havingValue = "test")
@EnableAutoConfiguration(exclude = {Neo4jTestHarnessAutoConfiguration.class})
public class EmbeddedNeo4JConfig {
    @Bean
    public Neo4j embeddedDatabaseServer() {
        log.info("starting Embedded Neo4J DB");
        Neo4j embeddedDatabaseServer = Neo4jBuilders.newInProcessBuilder()
                .withDisabledServer()
                .withConfig(GraphDatabaseSettings.procedure_allowlist, List.of("gds.*", "n10s.*", "apoc.*"))
                .withConfig(BoltConnector.listen_address, new SocketAddress(7687))
                .withConfig(GraphDatabaseSettings.procedure_unrestricted, List.of("gds.*", "n10s.*", "apoc.*"))
                // will be user for gds procedure
                .withProcedure(GraphExistsProc.class) // gds.graph.exists procedure
                .withProcedure(GraphListProc.class)
                .withProcedure(GraphProjectProc.class)
                // will be used for neo-semantics
                .withProcedure(GraphConfigProcedures.class) // n10s.graphconfig.*
                .withProcedure(RDFLoadProcedures.class)
                // will be used for apoc
                .withProcedure(Utils.class) // apoc.utils.*
                .build();
        log.info("started Embedded Neo4J DB: {}", embeddedDatabaseServer);
        return embeddedDatabaseServer;
    }

    @Bean(destroyMethod = "close")
    public Driver driver(Neo4j embeddedDatabaseServer) {
        Driver driver = GraphDatabase.driver(embeddedDatabaseServer.boltURI());
        Session session = driver.session();
        Result result = session.run("CALL gds.graph.exists('neo4j');");
        Result checkGraphInit = session.run("MATCH (n:_GraphConfig) WITH COUNT(n) > 0  as configExists RETURN configExists;");
        if (result.hasNext() && checkGraphInit.hasNext()) {
            org.neo4j.driver.Record record = result.next();
            org.neo4j.driver.Value value = record.get("exists");
            if (value != null && value.asBoolean()) {
                log.info("Graph already loaded");
                return driver;
            }
        }
        if (checkGraphInit.hasNext()) {
            org.neo4j.driver.Record recordGraphInit = checkGraphInit.next();
            org.neo4j.driver.Value graphExists = recordGraphInit.get("configExists");
            if (graphExists != null && graphExists.asBoolean()) {
                log.info("Graph already Initiated");
                return driver;
            }
        }
        session.run("CALL n10s.graphconfig.init({handleVocabUris:'MAP',handleMultival:\"ARRAY\",multivalPropList:[\"http://w3id.org/gaia-x/service#claimsGraphUri\"] });"); /// run only when creating a new graph
        session.run("CREATE CONSTRAINT n10s_unique_uri IF NOT EXISTS ON (r:Resource) ASSERT r.uri IS UNIQUE");
        log.info("n10 procedure and Constraints are loaded successfully");
        return driver;
    }
}
