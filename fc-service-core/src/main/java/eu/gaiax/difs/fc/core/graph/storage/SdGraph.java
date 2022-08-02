package eu.gaiax.difs.fc.core.graph.storage;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;



public class SdGraph {


	private String SelfDescription="";

	private File SelfDescriptionFile; /* File storing Self Description */;

	public SdGraph(File SelfDescriptionFile) {
		this.SelfDescriptionFile = SelfDescriptionFile;

	}



	public SdGraph() {
		try {
			File rootDirectory = new File("./");

			String rootDirectoryPath= rootDirectory.getCanonicalPath();
			//this.SelfDescriptionFile = new File(rootDirectoryPath+FilePath);

		}
		catch (Exception e) {
			System.out.println("File does not exist");
		}
	}

	public File getSelfDescriptionFile()
	{
		return SelfDescriptionFile;
	}




}
