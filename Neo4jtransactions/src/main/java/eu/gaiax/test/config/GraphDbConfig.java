package eu.gaiax.test.config;

import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
//@Configuration
//@ConditionalOnProperty(value = "federated-catalogue.scope", havingValue = "runtime")
public class GraphDbConfig {

    @Value("${graphstore.uri}")
    private String uri;
    @Value("${graphstore.user}")
    private String user;
    @Value("${graphstore.password}")
    private String password;

    @Bean(destroyMethod = "close")
    public Driver driver() {
        Driver driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
        Session session = driver.session();
        Result result = session.run("CALL gds.graph.exists('test');");
        if (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();
            org.neo4j.driver.Value value = record.get("exists");
            if (value != null && value.asBoolean()) {
                log.info("Graph already loaded");
                return driver;
            }
        }
        if (!session.run("CALL n10s.graphconfig.show();").hasNext()) {
            session.run("CALL n10s.graphconfig.init({handleVocabUris:'MAP',handleMultival:'ARRAY',multivalPropList:['http://w3id.org/gaia-x/service#claimsGraphUri']});"); /// run only when creating a new graph
            session.run("CREATE CONSTRAINT n10s_unique_uri IF NOT EXISTS ON (r:Resource) ASSERT r.uri IS UNIQUE");
             log.info("n10s.graphconfig.init() not called second time.");
        }
        log.info("n10 procedure and Constraints are loaded successfully");
        return driver;
    }

}
