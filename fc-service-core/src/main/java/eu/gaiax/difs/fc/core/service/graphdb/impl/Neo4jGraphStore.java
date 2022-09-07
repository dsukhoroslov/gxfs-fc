package eu.gaiax.difs.fc.core.service.graphdb.impl;

import eu.gaiax.difs.fc.core.exception.ServerException;
import eu.gaiax.difs.fc.core.pojo.OpenCypherQuery;
import eu.gaiax.difs.fc.core.pojo.SdClaim;
import eu.gaiax.difs.fc.core.service.graphdb.GraphStore;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.validator.routines.UrlValidator;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.internal.InternalNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log4j2
@Component
public class Neo4jGraphStore implements GraphStore {

    @Autowired
    private Driver driver;


    /**
     * Check if subject and predicate have valid uri
     *
     * @param sdClaim
     * @return True or False
     */
    public boolean validateTripleURI(SdClaim sdClaim) {
        UrlValidator urlValidator = new UrlValidator();
        if (urlValidator.isValid(sdClaim.stripSubject()) && urlValidator.isValid(sdClaim.stripPredicate())) {
            return true;
        } else {
            throw new RuntimeException("Enter a valid set of URI for claims "+sdClaim.asTriple());
        }
    }

    /**
     * Check if subject uri matches the credential subject
     *
     * @param subject
     * @param credentialSubject
     * @return true or false
     */
    public boolean validateCredentialSubject(String subject, String credentialSubject) {
        if (subject.equals(credentialSubject)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Validate the claims
     *
     * @param sdClaim
     * @param credentialSubject
     * @return Claims in a valid N-triples string format
     */
    public String validateClaims(SdClaim sdClaim, String credentialSubject) {
        Boolean tripleCheck = validateTripleURI(sdClaim);
        Boolean subjectCheck = validateCredentialSubject(sdClaim.stripSubject(), credentialSubject);
        if (tripleCheck && subjectCheck) {
            return sdClaim.asTriple();
        } else {
            log.debug("validation failed for : {}",sdClaim.asTriple());
            return null;
        }
    }

    /**
     * Process Query results from Neo4j Transaction to Map<string,string>
     * format
     *
     * @param result
     * @return List of String maps
     */
    public List<Map<String, String>> processResults(Result result) {
        List<Map<String, String>> resultList = new ArrayList<>();
        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();
            Map<String, Object> map = record.asMap();
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
        return resultList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addClaims(List<SdClaim> sdClaimList, String credentialSubject) {
        log.debug("addClaims.enter; got claims: {}, subject: {}", sdClaimList, credentialSubject);
        int cnt = 0;
        String payload = "";
        try (Session session = driver.session()) {
            for (SdClaim sdClaim : sdClaimList) {
                String validatedTriple = validateClaims(sdClaim, credentialSubject);
                payload = validatedTriple + sdClaim.asTriple();
                cnt++;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Map<String, String>> queryData(OpenCypherQuery sdQuery) {
        log.debug("queryData.enter; got query: {}", sdQuery);
        try (Session session = driver.session(); Transaction tx = session.beginTransaction()) {
            List<Map<String, String>> resultList = new ArrayList<>();
            Result result = tx.run(sdQuery.getQuery());
            resultList = processResults(result);
            log.debug("queryData.exit; returning: {}", resultList);
            return resultList;
        } catch (Exception e) {
            log.error("queryData.error", e);
            throw new ServerException("error querying data " + e.getMessage());
        }
    }

}

