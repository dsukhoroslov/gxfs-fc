The files in this directory can be used to demonstrate how to add a new schema to the catalogue. In this case the trusted cloud schema is used. To successfully do this it is necessary to have both the ontology graph, as well as the shapes graph. They have to be saved as .rdf files in order to successfully load them to the
To follow the example shown below make sure to remove the trusted-cloud schema files from the catalogue before starting.
The trusted-shapes ontology and shapes graphs are taken from the [GXFS Service Charateristics repository](https://gitlab.com/gaia-x/technical-committee/service-characteristics), but were manually adapted to fit this example.

Example use case: Validate a self description against the trusted-cloud schema.

1. Try to validate an invalid self description of the type trusted-cloud:ServiceOffering
    
Result: SD cannot be validated, since it is not a subclass of gax-core:ServiceOffering. This information is missing, because the trusted-cloud ontology graph is not yet uploaded to the catalogue.

2. Load the trusted-cloud ontology graph to the catalogue

Result: SD validates successfully, since the trusted-cloud shapes graph was not yet uplaoded to the catalogue.

3. Load the trusted-cloud shapes graph to the catalogue

Result: SD does not valdiate successfully, since a mandatory property is missing.

4. Add the missing property to the self description

Result: SD validates successfully. 

5. Try uploading the invalid self description to the catalogue

Result: No success.

6. Try uploading the invalid self description to the catalogue

Result: Success.