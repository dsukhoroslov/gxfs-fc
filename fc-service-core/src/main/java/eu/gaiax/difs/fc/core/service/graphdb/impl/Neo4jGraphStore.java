package eu.gaiax.difs.fc.core.service.graphdb.impl;

import eu.gaiax.difs.fc.core.exception.ClaimCredentialSubjectException;
import eu.gaiax.difs.fc.core.exception.ClaimSyntaxError;
import eu.gaiax.difs.fc.core.exception.ServerException;
import eu.gaiax.difs.fc.core.pojo.OpenCypherQuery;
import eu.gaiax.difs.fc.core.pojo.SdClaim;
import eu.gaiax.difs.fc.core.service.graphdb.GraphStore;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.jena.datatypes.DatatypeFormatException;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.*;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.eclipse.rdf4j.rio.ntriples.NTriplesParser;
import org.eclipse.rdf4j.rio.ntriples.NTriplesParserFactory;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.internal.InternalNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log4j2
@Component
public class Neo4jGraphStore implements GraphStore {

    @Autowired
    private Driver driver;

    // Stored to temporarily deviate from the standard Jena behavior of parsing
    // literals
    private boolean eagerJenaLiteralValidation;
    private boolean jenaAcceptanceOfUnknownLiteralDatatypes;

    /**
     * {@inheritDoc}
     */
    public static boolean validateCredentialSubject(String subject, String credentialSubject) {
        return subject.equals(credentialSubject);
    }

    private void switchOnJenaLiteralValidation() {
        // save the actual settings to not interfere with other modules which
        // rely on other settings
        eagerJenaLiteralValidation =
                org.apache.jena.shared.impl.JenaParameters.enableEagerLiteralValidation;
        jenaAcceptanceOfUnknownLiteralDatatypes =
                org.apache.jena.shared.impl.JenaParameters.enableSilentAcceptanceOfUnknownDatatypes;

        // Now switch to picky mode
        org.apache.jena.shared.impl.JenaParameters.enableEagerLiteralValidation = true;
        org.apache.jena.shared.impl.JenaParameters.enableSilentAcceptanceOfUnknownDatatypes = false;
    }

    private void resetJenaLiteralValidation() {
        org.apache.jena.shared.impl.JenaParameters.enableEagerLiteralValidation =
                eagerJenaLiteralValidation;
        org.apache.jena.shared.impl.JenaParameters.enableSilentAcceptanceOfUnknownDatatypes =
                jenaAcceptanceOfUnknownLiteralDatatypes;
    }

    private boolean validateRDFTripleSyntax(SdClaim claim) {
        Model model = ModelFactory.createDefaultModel();
        try (InputStream in = IOUtils.toInputStream(claim.asTriple(), "UTF-8")) {
            switchOnJenaLiteralValidation();
            RDFDataMgr.read(model, in, Lang.NT);

        } catch (IOException e) {
            // TODO: How to consistently log syntax errors in input data? DEBUG, WARN, ERR?
            log.debug(e);
            throw new ClaimSyntaxError("Syntax error in triple " + claim.asTriple());

        } catch (DatatypeFormatException e) {
            // Only occurs if the value of a literal does not comply with the
            // literals datatype
            throw new ClaimSyntaxError(
                    "Object in triple " +
                            claim.asTriple() +
                            " has an invalid value given its datatype"
            );

        } catch (RiotException e) {
            throw new ClaimSyntaxError(
                    "Object in triple " + claim.asTriple() +
                            " has a syntax error: " + e.getMessage()
            );

        } finally {
            resetJenaLiteralValidation();
        }

        UrlValidator urlValidator = UrlValidator.getInstance();
        for (ExtendedIterator<Triple> it = model.getGraph().find(); it.hasNext(); ) {
            Triple triple = it.next();

            Node s = triple.getSubject();
            if (!s.isURI() || !urlValidator.isValid(s.getURI())) {
                throw new ClaimSyntaxError(
                        "Subject in triple " +
                                claim.asTriple() +
                                " is not a valid URI");
            }

            Node p = triple.getPredicate();
            if (!p.isURI() || !urlValidator.isValid(p.getURI())) {
                throw new ClaimSyntaxError(
                        "Predicate in triple " +
                                claim.asTriple() +
                                " is not a valid URI");
            }

            Node o = triple.getObject();
            if (o.isURI()) {
                if (!urlValidator.isValid(o.getURI())) {
                    throw new ClaimSyntaxError(
                            "Object in triple " +
                                    claim.asTriple() +
                                    " is not a valid URI"
                    );
                }

            } else if (o.isLiteral()) {
                // Nothing needs to be done here as literal syntax errors and
                // datatype errors are already handled by the parser directly.
                // See the catch blocks after the RDFDataMgr.read( ) call above.

            } else {
                // assuming that blank nodes are not allowed
                throw new ClaimSyntaxError(
                        "Object in triple " +
                                claim.asTriple() +
                                " is neither a valid literal nor a valid URI"
                );
            }
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    protected String validateClaims(SdClaim sdClaim, String credentialSubject) {
        // Will throw a claim syntax error in case of syntax errors which is
        // handled by the calling function addClaim
        validateRDFTripleSyntax(sdClaim);

        // If we're here, there are no syntax errors. We now check whether the
        // credential subject is indeed used in the claim
        boolean subjectCheck = validateCredentialSubject(
                sdClaim.stripSubject(), credentialSubject);

        if (subjectCheck) {
            return sdClaim.asTriple();

        } else {
            // TODO: How to consistently log syntax errors in input data? DEBUG, WARN, ERR?
            String msg = "Credential subject validation failed for: " +  sdClaim.asTriple();

            log.debug(msg);
            throw new ClaimCredentialSubjectException(msg);
        }
    }

    /**
     * {@inheritDoc}
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
            throw new ServerException(
                    "error adding claims: " + e.getClass() + ": " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteClaims(String credentialSubject) {
        String claim = "";
        log.debug("deleteClaims.enter; Beginning claims deletion, subject: {}", credentialSubject);
        String query = "MATCH (n{uri: '" + credentialSubject + "'})\n" +
                "DELETE n";
        String checkClaim = "match(n) where n.uri ='" + credentialSubject + "' return n;";
        OpenCypherQuery checkQuery = new OpenCypherQuery(checkClaim);
        List<Map<String, String>> result = queryData(checkQuery);
        if (result != null && result.isEmpty()) {
            throw new ServerException("claim with credential subject " + credentialSubject + " not found");
        } else {
            Map<String, String> resultCheck = result.get(0);
            claim = resultCheck.entrySet().iterator().next().getValue();
        }
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

