import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.util.FileManager;
import org.apache.jena.util.PrintUtil;


public class Q2_7_a_inference {

	public static void printStatements(Model m, Resource s, Property p, Resource o) throws IOException {
		
		File file = new File("2.7-a-result.txt");
		if(!file.exists()) {
			System.out.println("output file does not exist. create new file...");
			file.createNewFile();
		}
		
		FileWriter fw = new FileWriter(file.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);
		
		System.out.println("start iterating through statements....");
		for(StmtIterator i = m.listStatements(s, p, o); i.hasNext(); ) {
			Statement stmt = i.nextStatement();
			System.out.println(" - " + PrintUtil.print(stmt));
			bw.write(PrintUtil.print(stmt) + "\n");
		}
		bw.close();
		System.out.println("done");
	}
	
	
	public static void main(String[] args) {
		
		//Output file
		
		System.out.println("load schema and data files....");
		Model schema = FileManager.get().loadModel("2.6-b-gedcom.xml");
		Model data = FileManager.get().loadModel("royal92.owl");
		Reasoner reasoner = ReasonerRegistry.getOWLReasoner();
		reasoner = reasoner.bindSchema(schema);
		InfModel infModel = ModelFactory.createInfModel(reasoner, data);
		
		//get Beatrix the Netherlands (#I661)
		Resource beatrix = infModel.getResource("http://www.daml.org/2001/01/gedcom/royal92#@I661@");
		System.out.println("beatrix of Netherlands *:");
		try {
			printStatements(infModel, beatrix, null, null);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
