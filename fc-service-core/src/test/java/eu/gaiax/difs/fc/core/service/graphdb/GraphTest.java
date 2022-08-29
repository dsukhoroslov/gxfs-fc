package eu.gaiax.difs.fc.core.service.graphdb;

import java.io.*;
import java.util.*;

import n10s.graphconfig.GraphConfigProcedures;
import n10s.rdf.load.RDFLoadProcedures;
import org.junit.FixMethodOrder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.runners.MethodSorters;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.configuration.connectors.BoltConnector;
import org.neo4j.configuration.helpers.SocketAddress;

import org.neo4j.gds.catalog.GraphExistsProc;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import eu.gaiax.difs.fc.core.pojo.GraphQuery;
import eu.gaiax.difs.fc.core.pojo.SdClaim;
import eu.gaiax.difs.fc.core.service.graphdb.impl.Neo4jGraphStore;
import eu.gaiax.difs.fc.core.config.GraphDbConfig;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GraphTest {
	private static final List<String> PROCEDURE_LIST = List.of("apoc.trigger.*", "gds.*", "n10s.*", "graph-data-science.*");

	private Neo4jGraphStore graphGaia;

	private static Neo4j embeddedDatabaseServer;

	@BeforeAll
	void initializeNeo4j() throws Exception {
		embeddedDatabaseServer = Neo4jBuilders.newInProcessBuilder()
				.withDisabledServer()
				.withConfig(GraphDatabaseSettings.procedure_allowlist, PROCEDURE_LIST)
				.withConfig(BoltConnector.listen_address, new SocketAddress(7687))
				.withConfig(GraphDatabaseSettings.procedure_unrestricted, PROCEDURE_LIST)
				.withProcedure(GraphExistsProc.class)
				.withProcedure(GraphConfigProcedures.class)
				.withProcedure(RDFLoadProcedures.class)
				.build();

		GraphDbConfig graphDbConfig = new GraphDbConfig();
		graphDbConfig.setUri(String.valueOf(embeddedDatabaseServer.boltURI()));
		graphDbConfig.setUser("neo4j");
		graphDbConfig.setPassword("12345");
		graphGaia = new Neo4jGraphStore(graphDbConfig);
	}

	@AfterAll
	void closeNeo4j() {
		embeddedDatabaseServer.close();
	}

	@DynamicPropertySource
	static void neo4jProperties(DynamicPropertyRegistry registry) {
		registry.add("org.neo4j.driver.uri", embeddedDatabaseServer::boltURI);
		registry.add("org.neo4j.driver.authentication.password", () -> "");
	}

	/**
	 * Data hardcoded for claims and upload to Graph . Given set of credentials,
	 * connect to graph and upload self description. Instantiate list of claims from
	 * file with subject predicate and object in N-triples form and upload to graph.
	 */
	@Test
	void simpleGraphUpload() throws Exception {
		List<SdClaim> sdClaimList = new ArrayList<>();
		SdClaim sdClaim = new SdClaim("<https://delta-dao.com/.well-known/participantCompany.json>",
				"<https://www.w3.org/2018/credentials#credentialSubject>",
				"<https://delta-dao.com/.well-known/participantCompany.json>");
		sdClaimList.add(sdClaim);
		Assertions.assertEquals("SUCCESS", graphGaia.uploadSelfDescription(sdClaimList));

	}

	/**
	 * Data hardcoded for claims and upload to Graph . Given set of credentials,
	 * connect to graph and upload self description. Instantiate list of claims from
	 * file with subject predicate and object in N-triples form and upload to graph.
	 */
	@Test
	void testLiteralGraphUpload() {
		String tripleString = "<https://delta-dao.com/.well-known/participantCompany.json> <gx-participant:registrationNumber> \"LURCSL.B186284\"^^<http://www.w3.org/2001/XMLSchema#string>";
		List<SdClaim> sdClaimList = new ArrayList<>();
		SdClaim sdClaim = new SdClaim("<https://delta-dao.com/.well-known/participantCompany.json>",
				"<https://www.w3.org/2018/credentials#credentialSubject>",
				"\"410 Terry Avenue North\"^^<http://www.w3.org/2001/XMLSchema#string>");
		sdClaimList.add(sdClaim);
		Assertions.assertEquals("SUCCESS", graphGaia.uploadSelfDescription(sdClaimList));
	}

	/**
	 * Query to graph using Query endpoint by instantiating query object and passing
	 * query string as parameter. THe result is a list of maps
	 *
	 * @throws Exception
	 */
	@Test
	@DisplayName("Test for QueryData")
	void testQueryTransactionEndpoint() throws Exception {
		List<SdClaim> sdClaimList = loadTestClaims();
		Assertions.assertEquals("SUCCESS", graphGaia.uploadSelfDescription(sdClaimList));

		List<Map<String, String>> resultList = new ArrayList<Map<String, String>>();
		Map<String, String> map = new HashMap<String, String>();
		map.put("n.ns0__country", null);
		map.put("n.ns0__legalName", "CompanyWebServicesEMEASARL");
		resultList.add(map);
		GraphQuery query = new GraphQuery(
				"match(n{ns0__legalName: 'CompanyWebServicesEMEASARL'}) return n.ns0__country, n.ns0__legalName;");
		List<Map<String, String>> response = graphGaia.queryData(query);
		Assertions.assertEquals(resultList, response);

	}

	private List<SdClaim> loadTestClaims() throws Exception {
		try (InputStream is = new ClassPathResource("Databases/neo4j/data/Triples/testData2.nt")
				.getInputStream()) {
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String strLine;
			List<SdClaim> sdClaimList = new ArrayList<>();
			while ((strLine = br.readLine()) != null) {
				String[] split = strLine.split("\\s+");
				SdClaim sdClaim = new SdClaim(split[0], split[1], split[2]);
				sdClaimList.add(sdClaim);

			}
			return sdClaimList;
		} catch (Exception e) {
			throw e;
		}
	}
}