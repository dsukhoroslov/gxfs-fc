The files in this directory can be used to demonstrate how to add a new schema to the Catalogue.
In this case, the schema for the [Trusted Cloud criteria](https://www.trusted-cloud.de/) is used. To successfully do this, it is necessary to have both the ontology graph, as well as the shapes graph.
They currently have to be saved as files with the filename extension `.rdf` in order to successfully load them into the Catalogue but can use any supported serialization.

To follow the example shown below, make sure to remove any Trusted Cloud schemas from the Catalogue before starting.
The `trusted-shapes` ontology and shapes graphs are taken from the [repository of the Gaia-X Working Group Service Characteristics](https://gitlab.com/gaia-x/technical-committee/service-characteristics), to which GXFS contributed them, but they were manually adapted to fit this example.

Example use case: Validate a Self-Description against the Trusted Cloud schema.

1. Try to validate an invalid Self-Description of the type `trusted-cloud:ServiceOffering`
    
Result: SD cannot be validated, since it is not a subclass of `gax-core:ServiceOffering`. This information is missing, because the Trusted Cloud ontology graph is not yet uploaded to the Catalogue.

2. Load the Trusted Cloud ontology graph to the Catalogue

Result: SD validates successfully, since the Trusted Cloud shapes graph was not yet uploaded to the Catalogue.

3. Load the Trusted Cloud shapes graph to the Catalogue

Result: SD does not validate successfully, since a mandatory property is missing.

4. Add the missing property to the Self-Description

Result: SD validates successfully. 

5. Try uploading the invalid Self-Description to the Catalogue

Result: No success.

6. Try uploading the invalid Self-Description to the Catalogue

Result: Success.
