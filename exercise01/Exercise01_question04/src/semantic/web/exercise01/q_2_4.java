package semantic.web.exercise01;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.FileManager;
import org.apache.log4j.Logger;

/**
 * see http://stackoverflow.com/questions/22846026/how-to-query-an-ontology-file-with-jena-on-eclipse
 * @author Arif
 *
 */

public class q_2_4 {
	public static final String inputFile = "royal92.nt";
	public static final String NL = System.getProperty("line.separator");
	private static final Logger log = Logger.getLogger("royal");
	
	public static void main(String[] args) {
		//create the (simplest) model
		final Model model = ModelFactory.createDefaultModel();
		
		//use the final manager to read an RDF document into the model
		FileManager.get().readModel(model, inputFile);
		log.debug("load a model with no. statements = " + model.size());
		
		//create query
		final String prolog1 = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>";
		final String prolog2 = "PREFIX a: <http://www.daml.org/2001/01/gedcom/gedcom#>";
		
		//this is the query for question 2.1a
		final String queryString = prolog1 + NL + prolog2 + NL + 
				"SELECT DISTINCT ?x WHERE { ?y a:sex \"F\" . ?y a:title ?x .}";
		
		final Query query = QueryFactory.create(queryString);
		
		//print the query with line numbers
		query.serialize(new IndentedWriter(System.out, true));
		System.out.println();
		
		//create a single execution of this query, and then apply to a model
		//which is wrapped up as a data sets
		final QueryExecution qexec = QueryExecutionFactory.create(query, model);
		System.out.println("All royal titles: ");
		System.out.println("--------------------");
		try {
			//assumption: it is a SELECT query
			final ResultSet rs = qexec.execSelect();
			//the order of results is undefined
			int counter = 1;
			for(; rs.hasNext();) {
				final QuerySolution rb = rs.nextSolution();
				//get title - variable names do not include the '?'
				final RDFNode x = rb.get("x");
				System.out.println(counter + ". " + x);
				counter++;
			}
		} finally {
			qexec.close();
		}
	}

}
