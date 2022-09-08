package eu.gaiax.difs.fc.core.service.graphdb.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.cypher.internal.ast.Statement;
import org.neo4j.cypher.internal.parser.CypherParser;
import org.neo4j.cypher.internal.util.OpenCypherExceptionFactory;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.internal.InternalNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.gaiax.difs.fc.core.exception.ServerException;
import eu.gaiax.difs.fc.core.pojo.OpenCypherQuery;
import eu.gaiax.difs.fc.core.pojo.SdClaim;
import eu.gaiax.difs.fc.core.service.graphdb.GraphStore;
import lombok.extern.log4j.Log4j2;
import scala.Option;

@Log4j2
@Component
public class Neo4jGraphStore implements GraphStore {

    @Autowired
    private Driver driver;

    /**
     * {@inheritDoc}
     */
    @Override
    public void addClaims(List<SdClaim> sdClaimList, String credentialSubject) {
        log.debug("addClaims.enter; got claims: {}, subject: {}", sdClaimList, credentialSubject);
        String payload = "";
        int cnt = 0;
        try (Session session = driver.session()) {
            for (SdClaim sdClaim : sdClaimList) {
                String subject = sdClaim.stripSubject();
                if (subject.equals(credentialSubject)) {
                    payload = payload + sdClaim.asTriple();
                    cnt++;
                }
            }

            String query = " WITH '\n" + payload + "' as payload\n"
                    + "CALL n10s.rdf.import.inline(payload,\"N-Triples\") YIELD terminationStatus, triplesLoaded, triplesParsed, namespaces, extraInfo\n"
                    + "RETURN terminationStatus, triplesLoaded, triplesParsed, namespaces, extraInfo";

            log.debug("addClaims; Query: {}", query);
            Result rs = session.run(query);
            log.debug("addClaims.exit; claims added: {}, results: {}", cnt, rs.list());
        } catch (Exception e) {
            log.error("addClaims.error", e);
            throw new ServerException("error adding claims: " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteClaims(String credentialSubject) {
        log.debug("deleteClaims.enter; Beginning claims deletion, subject: {}", credentialSubject);
        String query = "MATCH (n{uri: '" + credentialSubject + "'})\n" +
                "DELETE n";
        String checkClaim = "match(n) where n.uri ='" + credentialSubject + "' return n;";
        OpenCypherQuery checkQuery = new OpenCypherQuery(checkClaim);
        List<Map<String, String>> result = queryData(checkQuery);
        Map<String, String> resultCheck = result.get(0);
        String claim = resultCheck.entrySet().iterator().next().getValue();

        try (Session session = driver.session()) {
            if (claim.equals(credentialSubject)) {
                session.run(query);
                log.debug("deleteClaims.exit; Deleting executed successfully ");
            } else {
                log.debug("deleteClaims.exit; Claim does not exist in GraphDB ");
            }
        }
    }

    protected static boolean doesDataManipulation(String query) throws OpenCypherExceptionFactory.SyntaxException {
        CypherParser parser = new CypherParser();
        Statement statement = parser.parse(
                query,
                new OpenCypherExceptionFactory(Option.empty()),
                Option.empty()
        );

        return statement.containsUpdates();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Map<String, String>> queryData(OpenCypherQuery sdQuery) {
        log.debug("queryData.enter; got query: {}", sdQuery);

        /* Check the query
         * Will result in a SyntaxException if the query is syntactically wrong.
         * It is further checked if the query tries to modify any data.
         */
        try {
            if (doesDataManipulation(sdQuery.getQuery())) {
                log.error("Got a query which tries to modify data");
                throw new ServerException(
                        "Queries that modify data are not allowed");
            }

        } catch (OpenCypherExceptionFactory.SyntaxException e) {
            log.error("queryData.error", e);
            throw new ServerException("error querying data " + e.getMessage());
        }

        try (Session session = driver.session(); Transaction tx = session.beginTransaction()) {
            List<Map<String, String>> resultList = new ArrayList<>();
            Result result = tx.run(sdQuery.getQuery());
            log.debug("queryData; got result: {}", result.keys());
            while (result.hasNext()) {
                org.neo4j.driver.Record record = result.next();
                Map<String, Object> map = record.asMap();
                log.debug("queryData; record: {}", map);
                Map<String, String> outputMap = new HashMap<String, String>();
                for (var entry : map.entrySet()) {
                    if (entry.getValue() instanceof String) {
                        outputMap.put(entry.getKey(), entry.getValue().toString());
                    } else if (entry.getValue() == null) {
                        outputMap.put(entry.getKey(), null);
                    } else if (entry.getValue() instanceof InternalNode) {
                        InternalNode SDNode = (InternalNode) entry.getValue();
                        outputMap.put("n.uri", SDNode.get("uri").toString().replace("\"", ""));
                    }
                }
                resultList.add(outputMap);
            }
            log.debug("queryData.exit; returning: {}", resultList);
            return resultList;
        } catch (Exception e) {
            log.error("queryData.error", e);
            throw new ServerException("error querying data " + e.getMessage());
        }
    }

}

