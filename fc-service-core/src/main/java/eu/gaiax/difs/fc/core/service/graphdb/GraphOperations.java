package eu.gaiax.difs.fc.core.service.graphdb;

import eu.gaiax.difs.fc.core.pojo.GraphQuery;
import eu.gaiax.difs.fc.core.pojo.SdClaim;

import java.util.List;
import java.util.Map;

public interface GraphOperations {


    /**
     * Pushes set of claims to the Graph db. The set of claims are list of claim
     * object containing subject, predicate and object in the form of ntriples
     * format stored in individual strings.
     *
     * @param sdClaimList List of claims to add to the Graph DB.
     * @return String SUCCESS or FAIL
     */
    public String uploadSelfDescription(List<SdClaim> sdClaimList);

    /**
     * Query the graph when  Cypher query is passed in query object and this
     * returns list of Maps with key value pairs as a result.
     *
     * @param sdQuery Query to execute
     * @return List of Maps
     */
    public List<Map<String, Object>> queryData(GraphQuery sdQuery);


}

