package eu.gaiax.difs.fc.core.service.graphdb.impl;

import eu.gaiax.difs.fc.core.pojo.GraphQuery;
import eu.gaiax.difs.fc.core.pojo.GraphSchema;
import eu.gaiax.difs.fc.core.pojo.SdClaim;
import eu.gaiax.difs.fc.core.service.graphdb.GraphOperations;
import eu.gaiax.difs.fc.core.service.graphdb.GraphPerformance;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.neo4j.driver.*;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GraphConnect implements AutoCloseable, GraphOperations, GraphPerformance {

    private final Driver driver;



    public GraphConnect() {
        this("bolt://localhost:7687", "neo4j", "12345");

    }

    public GraphConnect(String uri, String user, String password) {

        driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
        System.out.println("connected");
        initialiseGraph(driver);

    }

    @Override
    public void close() throws Exception {
        driver.close();
    }

    private void initialiseGraph(Driver driver) {
        try (Session session = driver.session()) {
            session.run("CALL n10s.graphconfig.init();"); /// run only when creating a new graph
            session.run("CREATE CONSTRAINT n10s_unique_uri ON (r:Resource) ASSERT r.uri IS UNIQUE");
            System.out.println("n10 procedure and Constraints are loaded successfully");
        } catch (org.neo4j.driver.exceptions.ClientException e) {
            System.out.println("Graph already loaded" + e);
        }
    }

    public float QueryPerformance() {
        Instant starts = Instant.now();
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            System.out.println("An  Exception occurred: " + e);
        }
        Instant ends = Instant.now();
        System.out.println(Duration.between(starts, ends));

        return 0;
    }


    public List<Map<String, Object>> queryData(GraphQuery sdQuery) {
        /*
         * Given a query object , the method executes the query on the neo4j database
         * and returns the list of maps as output
         */


        try (Session session = driver.session()) {
            List<Map<String, Object>> Result_list = new ArrayList();
            System.out.println("beginning transaction");
            Transaction tx = session.beginTransaction();
            Result result = tx.run(sdQuery.getQuery());
            String SD_query_op = "";
            while (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> map = record.asMap();
                Result_list.add(map);
            }
            System.out.println("Query executed successfully ");
            return Result_list;
        } catch (Exception e) {
            System.out.println("Query unsuccessful " + e);
            return null;
        }
    }

    public String GetSchema() throws Exception {
        List<String> schema_statement = new ArrayList<String>();
        try (Session session = driver.session()) {
            String urlString = "http://localhost:7474/rdf/neo4j/onto?format=JSON-LD";
            String loginPassword = "neo4j:12345";
            URL url = new URL(urlString);
            Base64 base64 = new Base64();
            String encodedString = new String(base64.encode(loginPassword.getBytes()));
            URLConnection conn = url.openConnection();
            conn.setRequestProperty("Authorization", "Basic " + encodedString);
            InputStream is = new BufferedInputStream(conn.getInputStream());
            String schema = IOUtils.toString(is, "UTF-8");
            return schema;

        } catch (Exception e) {
            return "Exception in returning schema " + e;

        }

    }

    public void AddNamespaces(Map<String, String> map) {
        try (Session session = driver.session()) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                session.run("CALL n10s.nsprefixes.add('" + entry.getKey() + "','" + entry.getValue() + "')");

            }
        }
    }


    public Set<String> listFilesUsingJavaIO(String dir) {
        /*
         * For a given directory passed as a string, the function returns a list of
         * files in the directory
         */

        System.out.println(Stream.of(new File(dir).listFiles()).filter(file -> !file.isDirectory()).map(File::getName)
                .collect(Collectors.toSet()));
        return Stream.of(new File(dir).listFiles()).filter(file -> !file.isDirectory()).map(File::getName)
                .collect(Collectors.toSet());

    }

    public String uploadSchema(GraphSchema graphSchema) {
        /*
         * Given Schema object with schema payload passed as string, we extract namespaces from file
         * and populate it after which we load the SHACL file. Returns if successful or
         * not
         */

        try (Session session = driver.session()) {
            String payload = graphSchema.getSchema();
            File rootDirectory = new File("./");
            String rootDirectoryPath = rootDirectory.getCanonicalPath();
            String fpath = "/FS-Service/fc-service/fc-service-core/src/test/resources/Databases/neo4j/data/legal-personShape.ttl";
            FileInputStream in = new FileInputStream(rootDirectoryPath+fpath);
            Model model = RDFDataMgr.loadModel(rootDirectoryPath+fpath);
            Map<String, String> map;
            map = model.getNsPrefixMap();
            AddNamespaces(map);
            String query = " WITH '\n" +
                    payload +
                    "' as payload\n" +
                    "CALL n10s.validation.shacl.import.inline(payload,\"Turtle\") YIELD triplesLoaded\n" +
                    "RETURN triplesLoaded";
            System.out.println(query);
            session.run(query);
            return "SUCCESS";
        } catch (Exception e) {
            System.out.println("Exeption is " + e);
            return "FAIL";
        }
    }


    public String uploadSelfDescription(List<SdClaim> sdClaimList) {
		/* Pass claim as a pojo object with subject, predicate and object. This is in turn passed on to the function which uploads it to the neo4j database.
		  Function returns SUCCESS or FAIL  */

        String payload = "";

        try (Session session = driver.session()) {
            for (SdClaim sdClaim : sdClaimList) {
                payload = payload + sdClaim.subject + " " + sdClaim.predicate + " " + sdClaim.object + "	. \n";

            }

            String query = " WITH '\n" +
                    payload +
                    "' as payload\n" +
                    "CALL n10s.rdf.import.inline(payload,\"N-Triples\") YIELD terminationStatus, triplesLoaded\n" +
                    "RETURN terminationStatus, triplesLoaded";

            System.out.println(query);
            session.run(query);
            return "SUCCESS";
        } catch (Exception e) {
            return "FAIL";
        }
    }

}
