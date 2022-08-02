package eu.gaiax.difs.fc.core.graph.storage;

public class SdSchema {

	String SelfDescription_schema_path = "/home/nacharya/federated-catalogue/Graph-Interface/SD-Graph/Data/Schema/legal-person-schema/"; /*
																																			 * String
																																			 * which
																																			 * specifies
																																			 * Schema
																																			 * file
																																			 * path
																																			 */

	String schema_input_serialisation = "Turtle";

	String schema_output_serialisation = "Turtle";

	String schema_input_format = "File";

	String schema_output_format = "String";

	public void SetSchemaFilePath(String input_file) {
		/* Set directory path for schema file */

		SelfDescription_schema_path = input_file;
	}

	public String GetSchemaFilePath() {
		/* Get file path of schema file */

		return SelfDescription_schema_path;

	}

	public void SetSchemaInputSerialisation(String input_serialisation) {
		/* Set input serialisation format for schema file */

		schema_input_serialisation = input_serialisation;
	}

	public String GetSchemaInputSerialisation() {
		/* Get input serialisation format for schema file */

		return schema_input_serialisation;
	}

	public void SetSchemaOutputSerialisation(String output_serialisation) {
		/* Set output serialisation format for output schema file */

		schema_output_serialisation = schema_output_serialisation;
	}

	public String GetSchemaOutputSerialisation() {
		/* Get output serialisation format for output schema file */

		return schema_output_serialisation;
	}

	public void SetSchemaInputFormat(String input_format) {
		/* Set input format for schema */

		schema_input_format = input_format;
	}

	public String GetSchemaInputFormat() {
		/* Get input format for schema */

		return schema_input_format;
	}

	public void SetSchemaOutputFormat(String output_format) {
		/* Set output format for schema */

		schema_output_format = output_format;
	}

	public String GetSchemaOutputFormat() {
		/* Get output format for schema */

		return schema_output_format;
	}

}
