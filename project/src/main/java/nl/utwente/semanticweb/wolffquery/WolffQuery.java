package nl.utwente.semanticweb.wolffquery;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.util.FileManager;
import org.apache.jena.util.PrintUtil;

public class WolffQuery {
	
	private static final String ONTOLOGY = "wolff.owl";
	private static final String RDF_DATA = "wolff.rdf";
		
	public static void main(String[] args) {
		OntModel ont = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_RULE_INF);
		ont.read(ONTOLOGY, "RDF/XML");
		
		Reasoner reasoner = ReasonerRegistry.getOWLReasoner();
		reasoner = reasoner.bindSchema(ont);
		
//		Model schema = FileManager.get().loadModel(ONTOLOGY);
		Model data = FileManager.get().loadModel(RDF_DATA);
		
		
		InfModel infModel = ModelFactory.createInfModel(reasoner, data);
		
		// Query: which movie is starred by Monica Bellucci? 
		//        in RDF file, movie Spectre has property 'hasActor' which refers to Monica Bellucci.
		// 		  In the ontology file, the inverse of 'hasActor' is defined by 'becomesActorOf'.
		//        Thus, the query below should result in Spectre (due to the inference).
		//		  But somehow, it shows nothing.
		//		  (Continued in the next comment)
		String queryString = "PREFIX cat: <http://dbpedia.org/resource/Category:> " +
			  	 "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> " +
			  	 "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
			  	 "PREFIX owl: <http://www.w3.org/2002/07/owl#> " +
			  	 "PREFIX foaf: <http://xmlns.com/foaf/0.1/> " + 
			  	 "PREFIX dbo: <http://dbpedia.org/ontology/> " +
			  	 "PREFIX : <http://dbpedia.org/resource/>" + 
			  	 "PREFIX dbpedia2: <http://dbpedia.org/property/>" + 
			  	 "PREFIX wolff: <http://www.wolff.nl/2016/wolff.owl#>" + 
			  	 "PREFIX wolff-p: <http://www.wolff.nl/person#>" +
			  	 "SELECT ?movie WHERE {"+
		  			"<http://www.wolff.nl/person#Monica_Bellucci> <http://www.wolff.nl/2016/wolff.owl#becomesActorOf> ?movie ." +
			  	 "}";
		Query query = QueryFactory.create(queryString);
		QueryExecution qe = QueryExecutionFactory.create(query, infModel);
		ResultSet results = qe.execSelect();
		
		ResultSetFormatter.out(System.out, results, query);
		
		// Surprisingly, the following lines result all inferences from resource 'Monica Bellucci', and
		// one of those is the inference that she becomesActorOf Spectre.
		// But this inference is not displayed in the previous SPARQL query.
		Resource belucci =  infModel.getResource("http://www.wolff.nl/person#Monica_Bellucci");
		for(StmtIterator i = infModel.listStatements(belucci, (Property)null, (RDFNode)null); i.hasNext(); ) {
			Statement stmt = i.nextStatement();
			System.out.println(" - " + PrintUtil.print(stmt));
		}
	}

}
