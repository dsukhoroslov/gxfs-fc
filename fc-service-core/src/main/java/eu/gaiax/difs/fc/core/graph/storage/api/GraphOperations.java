package eu.gaiax.difs.fc.core.graph.storage.api;
import java.util.List;

import eu.gaiax.difs.fc.core.graph.storage.GraphQuery;
import eu.gaiax.difs.fc.core.graph.storage.SdClaim;

public interface GraphOperations
{


    /*
    Pushes set of claims to the Graph db. The set of claims are list of claim object containing subject, predicate and object in the form of ntriples format stored in individual strings
    Example:
    subject="<https://delta-dao.com/.well-known/participantAmazon.json>";
    predicate="<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>";
    object="<https://json-ld.org/playground/LegalPerson>";
    SdClaim(subject, predidcate, object)
     */
    public String uploadSelfDescription(List<SdClaim> sdClaimList) throws Exception;



    /*
   Query the graph when  Cypher query is passed in query object and this returns list of string gson as a result
    selfDescriptionQuery = " MATCH (n)\n" + "RETURN n "
    GraphQuery(selfDescriptionQuery)
   * */
    public List<String> queryData(GraphQuery sdQuery);








}

