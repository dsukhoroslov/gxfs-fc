package eu.gaiax.difs.fc.core.graph.storage;

import eu.gaiax.difs.fc.core.graph.storage.GraphConnect;
import eu.gaiax.difs.fc.core.graph.storage.GraphQuery;
import eu.gaiax.difs.fc.core.graph.storage.SdClaim;
import org.junit.FixMethodOrder;
import org.junit.jupiter.api.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import org.junit.runners.MethodSorters;
import org.neo4j.driver.Session;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.MountableFile;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GraphTest {

    GraphConnect graphGaia;
    @Container
    final static Neo4jContainer<?> container = new Neo4jContainer<>("neo4j:4.4.5")
            .withNeo4jConfig("dbms.security.procedures.unrestricted", "apoc.*,n10s.*")
            .withEnv("NEO4JLABS_PLUGINS", "[\"apoc\",\"n10s\"]")
            .withEnv("NEO4J_dbms_security_procedures_unrestricted", "apoc.*")
            .withEnv("apoc.import.file.enabled", "true")
            .withEnv("apoc.import.file.use_neo4j_config", "false")
            .withAdminPassword("12345");

    @BeforeAll
    void setupContainer() {
        container.start();
        String url = container.getBoltUrl();
        String testurl = container.getHttpUrl();
        String user = "neo4j";
        String password = "12345";
        graphGaia = new GraphConnect(url, user, password);


    }

    @AfterAll
    void stopContainer() {
        container.stop();
    }

    @Test
    void GraphUploadSimulate() {
        /*
        Simulate Data from file upload on SD storage graph. Given set of credentials, connect to graph and upload self description.
        Instantiate list of claims from file with subject predicate and object and upload to graph.
        * */

        String url = container.getBoltUrl();
        String user = "neo4j";
        String password = "12345";
        //GraphConnect graphGaia = new GraphConnect(url,user,password);
        try {
            //graphGaia = new GraphConnect(url,user,password);
            File rootDirectory = new File("./");
            String rootDirectoryPath = rootDirectory.getCanonicalPath();
            String path = "/src/test/resources/Databases/neo4j/data/Triples/testData2.nt";
            FileInputStream fstream = new FileInputStream(rootDirectoryPath + path);
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String strLine;
            List<SdClaim> sdClaimList = new ArrayList<>();
            while ((strLine = br.readLine()) != null) {
                String[] split = strLine.split("\\s+");
                SdClaim sdClaim = new SdClaim(split[0], split[1], split[2]);
                sdClaimList.add(sdClaim);

            }
            Assertions.assertEquals("SUCCESS", graphGaia.uploadSelfDescription(sdClaimList));
            fstream.close();

        } catch (Exception e) {
            System.out.println("error " + e);
        }
    }


    @Test
    void GraphUploadSimple() {
        /*
         Data hardcoded for claims and upload to Graph . Given set of credentials, connect to graph and upload self description.
        Instantiate list of claims from file with subject predicate and object in N-triples form and upload to graph.
        * */


        try {
            List<SdClaim> sdClaimList = new ArrayList<>();
            SdClaim sdClaim = new SdClaim("<https://delta-dao.com/.well-known/participantAmazon.json>", "<https://www.w3.org/2018/credentials#credentialSubject>", "<https://delta-dao.com/.well-known/participantAmazon.json>");
            sdClaimList.add(sdClaim);
            Assertions.assertEquals("SUCCESS", graphGaia.uploadSelfDescription(sdClaimList));
        } catch (Exception e) {
            System.out.println(" failed");
        }

    }


    @Test
    @DisplayName("Test for QueryData")
    void testQueryTransactionEndpoint() {
        /*
         * Query to graph using Query endpoint by instantiating query object and passing query string as parameter. THe result is a list of gson strings */
        try {
            File rootDirectory = new File("./");
            String rootDirectoryPath = rootDirectory.getCanonicalPath();
            String path = "/src/test/resources/Databases/neo4j/data/Triples/testData2.nt";
            FileInputStream fstream = new FileInputStream(rootDirectoryPath + path);
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String strLine;
            List<SdClaim> sdClaimList = new ArrayList<>();
            while ((strLine = br.readLine()) != null) {

                System.out.println(strLine);
                String[] split = strLine.split("\\s+");
                SdClaim sdClaim = new SdClaim(split[0], split[1], split[2]);
                sdClaimList.add(sdClaim);

            }
            Assertions.assertEquals("SUCCESS", graphGaia.uploadSelfDescription(sdClaimList));
            fstream.close();

        } catch (Exception e) {
            System.out.println("error " + e);
        }

        List<String> Result_list = new ArrayList<String>();
        GraphQuery query = new GraphQuery("match(n{ns0__name:'AmazonWebServices'}) return n.ns0__legalName;");
        String jsonQuery = "{\"n.ns0__legalName\":\"AmazonWebServicesEMEASARL\"}";
        Result_list.add(jsonQuery);
        List<String> response = graphGaia.queryData(query);
        Assertions.assertEquals(Result_list, response);
        System.out.println(response);

    }

}