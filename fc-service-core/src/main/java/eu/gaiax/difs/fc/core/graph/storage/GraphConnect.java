package eu.gaiax.difs.fc.core.graph.storage;

import eu.gaiax.difs.fc.core.graph.storage.api.GraphOperations;
import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.rio.*;
import org.neo4j.driver.*;
import java.net.URL;
import java.net.URLConnection;
import org.apache.commons.codec.binary.Base64;
import org.neo4j.driver.Record;
import java.lang.String;
import java.io.*;
import java.time.Duration;
import java.time.Instant;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.Rio;
import com.google.gson.Gson;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GraphConnect implements AutoCloseable, GraphOperations, GraphPerformance {

	private final Driver driver;

	// TODO Add properties or variables in a separate file


	public GraphConnect() {
		this("bolt://localhost:7687", "neo4j", "12345");

	}

	public GraphConnect(String uri, String user, String password)
	{

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
			System.out.println("Graph already loaded"+e);
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


	public List<String> queryDataDeprecate(GraphQuery sdQuery) {
		/*
		 * Deprecated to avoid data manipulation operations in query interface
		 */

		try (Session session = driver.session()) {
			List<String> Result_list = new ArrayList<String>();
			Result result = session.run(sdQuery.getQuery());
			String SD_query_op = "";
			while (result.hasNext()) {
				Record record = result.next();
				SD_query_op = new Gson().toJson(record.asMap());
				Result_list.add(SD_query_op);
			}
			System.out.println("Query executed successfully "+Result_list);
			return Result_list;
		} catch (Exception e) {
			System.out.println("Query unsuccessful");
			return null;
		}
	}

	public List<String> queryData(GraphQuery sdQuery) {
		/*
		 * Given a query object , the method executes the query on the neo4j database
		 * and returns the list of json output
		 */


		try (Session session = driver.session()) {
			List<String> Result_list = new ArrayList<String>();
			System.out.println("beginning transaction");
			Transaction tx =session.beginTransaction();
			Result result = tx.run(sdQuery.getQuery());
			String SD_query_op = "";
			while (result.hasNext()) {
				Record record = result.next();
				SD_query_op = new Gson().toJson(record.asMap());
				Result_list.add(SD_query_op);
				}
			System.out.println("Query executed successfully "+Result_list);
			return Result_list;
		} catch (Exception e) {
			System.out.println("Query unsuccessful");
			return null;
		}
	}

	public String LoadSchema(SdSchema selfDescription) throws Exception {
		/*
		 * Given Graph object with schema file path, we extract namespaces from file
		 * amnd populate it after which we load the SHACL file. Returns if successful or
		 * not
		 */

		try (Session session = driver.session()) {
			System.out.println("Schema path is " + selfDescription.GetSchemaFilePath());
			Model model;
			FileInputStream in = new FileInputStream(selfDescription.GetSchemaFilePath());
			model = Rio.parse(in, RDFFormat.TURTLE);
			Set<Namespace> set = new HashSet<Namespace>();
			set = model.getNamespaces();
			AddNamespaces(set);
			session.run("CALL n10s.validation.shacl.import.fetch('file:" + selfDescription.GetSchemaFilePath()
					+ "','Turtle')");
			in.close();
			return "SUCCESS";
		} catch (Exception e) {
			return "Exception for file " + selfDescription.GetSchemaFilePath() + "which is " + e;

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

	public void AddNamespaces(Set<Namespace> set) {
		try (Session session = driver.session()) {
			for (Namespace s : set) {
				System.out.println(s.getPrefix());
				session.run("CALL n10s.nsprefixes.add('" + s.getPrefix() + "','" + s.getName() + "')");

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



	public String uploadSelfDescription(List<SdClaim> sdClaimList) throws Exception {
		/* Pass claim as a pojo object with subject, predicate and object. This is in turn passed on to the function which uploads it to the neo4j database.
		  Function returns SUCCESS or FAIL  */

		String payload="";

		try (Session session = driver.session()) {
			for (SdClaim sdClaim : sdClaimList) {
				payload=payload+sdClaim.subject + " " + sdClaim.predicate + " " + sdClaim.object + "	. \n";

			}

			String query = " WITH '\n"+
					         payload+
					         ///+ sdClaim.subject + " " + sdClaim.predicate + " " + sdClaim.object + "	. \n" +
					        "' as payload\n" +
					        "CALL n10s.rdf.import.inline(payload,\"N-Triples\") YIELD terminationStatus, triplesLoaded\n" +
					        "RETURN terminationStatus, triplesLoaded";

			System.out.println(query);
			session.run(query);
			return "SUCCESS";
		}
		catch (Exception e)
		{
			return "FAIL";
		}
	}

}
