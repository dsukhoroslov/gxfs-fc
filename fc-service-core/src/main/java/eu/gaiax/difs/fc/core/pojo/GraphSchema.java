package eu.gaiax.difs.fc.core.pojo;

public class GraphSchema {
    String selfDescriptionSchema;


    public GraphSchema(String selfDescriptionSchema) {
        this.selfDescriptionSchema = selfDescriptionSchema;
    }


    public String getSchema() {
        /* Get Neo4j Query to be executed*/

        return selfDescriptionSchema;
    }

}
