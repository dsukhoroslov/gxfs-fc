package eu.gaiax.difs.fc.core.graph.storage;

public class GraphQuery {
    String selfDescriptionQuery ;


    public GraphQuery()
    {
        this.selfDescriptionQuery = " MATCH (n)\n" + "RETURN n ";
    }

    public GraphQuery(String selfDescriptionQuery)
    {
        this.selfDescriptionQuery = selfDescriptionQuery;
    }


    public String getQuery()
    {
        /* Get Neo4j Query to be executed*/

        return selfDescriptionQuery;
    }

}
