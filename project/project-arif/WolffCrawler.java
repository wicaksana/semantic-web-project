package nl.utwente.semanticweb.wolffcrawler;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.RDF;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import com.sun.org.apache.xml.internal.security.utils.Base64;

public class WolffCrawler {
	
	private final static String WOLFF = "http://www.wolff.nl";
	private final static String WOLFF_OWL = WOLFF + "/2016/wolff.owl";
	private final static String NS = WOLFF_OWL + "#";
	private final static String MOVIE = WOLFF + "/movie" + "#";
	private final static String PERSON = WOLFF + "/person" + "#";
	private final static String GENRE = WOLFF + "/genre" + "#";
	private final static String COMPANY = WOLFF + "/company" + "#";
	private final static String REF_ONT = "wolff.owl";
	private final static String OUTPUT = "wolff.rdf";
	
	private final MessageDigest digest;
	
	private final Model m;
	private final OntModel refOnt;
	
	private final static String[] DUTCH_MONTH = {"januari", "februari", "maart", "april", "mei", "juni", "juli", "augustus", "september", "oktober", "november", "december"};
	private final static Map<String, String> LANGUAGE;
	static {
		LANGUAGE = new HashMap<String, String>();
		LANGUAGE.put("engels", "English");
		LANGUAGE.put("nederlands", "Dutch");
		LANGUAGE.put("turks", "Turkish");
		LANGUAGE.put("duits", "German");
	}
	
	private static Map<String, String> GENRE_MAP;
	static {
		GENRE_MAP = new HashMap<String, String>();
		GENRE_MAP.put("western", "Western");
		GENRE_MAP.put("drama", "Drama");
		GENRE_MAP.put("animatie", "Animation");
		GENRE_MAP.put("thriller", "Thriller");
		GENRE_MAP.put("romantiek", "Romance");
		GENRE_MAP.put("avontuur", "Adventure");
		GENRE_MAP.put("fantasy", "Fantasy");
		GENRE_MAP.put("comedy", "Comedy");
		GENRE_MAP.put("familie", "Family");
		GENRE_MAP.put("actie", "Action");
		
	}
	
	public WolffCrawler() throws NoSuchAlgorithmException {
		
		digest = MessageDigest.getInstance("SHA1");
		m = ModelFactory.createDefaultModel();
		m.setNsPrefix("wolff", NS);
		m.setNsPrefix("wolff-m", MOVIE);
		m.setNsPrefix("wolff-p", PERSON);
		m.setNsPrefix("wolff-g", GENRE);
		m.setNsPrefix("wolff-c", COMPANY);
		
		//load ontology file
		System.out.println("*) load ontology file....");
		refOnt = ModelFactory.createOntologyModel();
		try {
			refOnt.read(FileManager.get().open(REF_ONT), "", "RDF/XML");
		} catch(NullPointerException e) {
			e.printStackTrace();
		}
		
		//create genre manually
		for(String genre : GENRE_MAP.values()) {
			m.createResource(GENRE + genre)
			 .addProperty(RDF.type, refOnt.getProperty(NS + "Genre"));
		}
	}
	
	public static void main(String[] args) throws NoSuchAlgorithmException, IOException {
		
		WolffCrawler crawler = new WolffCrawler();
		crawler.parseIndex();
		crawler.save();
		
		System.out.println("---finish---");
	}
	
	private void save() throws FileNotFoundException {
		m.write(new FileOutputStream(OUTPUT), "RDF/XML");
	}

	private void parseIndex() throws IOException {
		Document doc = Jsoup.connect(WOLFF + "/bioscopen/cinestar/").get();
		Elements movies = doc.select(".table_agenda td[width=245] a");
		
		Set<String> urls = new HashSet<String>();
		for(Element movie : movies) {
			urls.add(movie.attr("href"));
		}
		
		for(String url: urls) {
			parseMovie(url);
		}
	}

	private void parseMovie(String url) {
		Boolean is3D = false;
		Boolean isDutch = false;
		String movietitle = url.replace("/films/", "");
		Document doc;
		try {
			doc = Jsoup.connect(WOLFF + url).get();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		// check if it is 3D movie, or dubbed in Dutch
		if(url.contains("-3d")) {
			movietitle = movietitle.replace("-3d", "");
			is3D = true;
		}
		if(url.contains("-nl")) {
			movietitle = movietitle.replace("-nl", "");
			isDutch = true;
		}
		
		//create resource movie with the type 'Movie' and data property 'title'
		Resource movie = m.createResource(MOVIE + movietitle);
		movie.addProperty(RDF.type, refOnt.getProperty(NS + "Movie"))
			 .addProperty(refOnt.getProperty(NS + "title"), doc.select("#wrapper_left h2").text().trim());
		
		// does m already contain resource 3D?
		if(!m.containsResource(refOnt.getProperty(NS + "3D"))) { 
			System.out.println("create 3D resource");
			m.createResource(NS + "3D")
			 .addProperty(RDF.type, refOnt.getProperty(NS + "Presentation"));
		}
		//if it is 3D movie..
		if(is3D)
			movie.addProperty(refOnt.getProperty(NS + "hasPresentation"), m.getResource(NS + "3D"));

		//parse sidebar information
		Elements sidebar = doc.select(".sidebar_container").get(1).select(".table_view tr td");
		for (Element element:sidebar) {
			switch (element.select("strong").text()) {
			case "Acteurs":
				String[] actors = element.text().substring(8).split(", "); //Remove "Acteurs" from string
				for(String actor : actors) {
					Resource person = m.createResource(PERSON + actor.replace(" ", "_"));
					person.addProperty(RDF.type, refOnt.getProperty(NS + "Person"))
						  .addProperty(refOnt.getProperty(NS + "name"), actor.trim());
					movie.addProperty(refOnt.getProperty(NS + "hasActor"), person);
				}
				break;
				
			case "Regie": //director
				String nameString = element.text().substring(6).toString().trim(); //Remove "Regie" from string
				Resource person = m.createResource(PERSON + encode(nameString));
				person.addProperty(refOnt.getProperty(NS + "name"), nameString);
				movie.addProperty(refOnt.getProperty(NS + "hasDirector"), person);
				break;

			case "Speelduur":
				movie.addProperty(refOnt.getProperty(NS + "duration"), last(element).toString().trim().split(" ")[0], XSDDatatype.XSDint);
				break;

			case "Genre":
				String[] genres = last(element).toString().toLowerCase().split(", ");
				for (String genreName:genres) {
					if(GENRE_MAP.containsKey(genreName))
						genreName = GENRE_MAP.get(genreName);
					else { //unknown genre; report it and create new resource to acommodate
						System.out.println("*) another genre found: " + genreName);
						m.createResource(GENRE + genreName)
						 .addProperty(RDF.type, refOnt.getProperty(NS + "Genre"));
					}						
					movie.addProperty(refOnt.getProperty(NS + "hasGenre"), m.getResource(GENRE + genreName));
				}					
				break;
			
			case "Taal":
				String lang = last(element).toString().trim().toLowerCase();
				if(LANGUAGE.containsKey(lang)) {
					lang = LANGUAGE.get(lang);
				} else {
					System.out.println("another language found: " + lang);
				}

				movie.addProperty(refOnt.getProperty(NS + "language"), lang);	
				break;
			
			case "In de bioscoop sinds":
				String[] releasedate = last(element).toString().trim().split(" ");
				String day = releasedate[0];
				String month = String.valueOf((Arrays.asList(DUTCH_MONTH).indexOf(releasedate[1].toLowerCase()) + 1));
				String year = releasedate[2];
				String formatteddate = year + "-" + month + "-" + day + "T00:00:00";
				movie.addProperty(refOnt.getProperty(NS + "releaseDate"), formatteddate, XSDDatatype.XSDdateTime);			
				break;
			
			case "Distributeur":
				Node distributorLink = (Node) last(element);
				Resource distributorResource;
				if (distributorLink instanceof Element) {
					distributorResource = m.createResource(COMPANY + ((Element) distributorLink).text().replace(" ", "_"));
					distributorResource.addProperty(refOnt.getProperty(NS + "companyURL"), distributorLink.attr("href"));
					distributorResource.addProperty(refOnt.getProperty(NS + "companyName"), ((Element) distributorLink).text());
					movie.addProperty(refOnt.getProperty(NS + "isDistributedBy"), distributorResource);
				} else {
					distributorResource = m.createResource(COMPANY + distributorLink.toString().replace(" ", "_"));
					distributorResource.addProperty(refOnt.getProperty(NS + "companyName"), distributorLink.toString());
				}
					
				movie.addProperty(refOnt.getProperty(NS + "isDistributedBy"), distributorResource);
				break;
			}
		}
		
	}

	/**
	 * Get last child node of element
	 * @param element
	 * @return
	 */
	private Object last(Element element) {
		List<Node> nodes = element.childNodes();
		return nodes.get(nodes.size()-1);
	}

	/**
	 * Encode string to create random url per name
	 * @param name
	 * @return
	 */
	private String encode(String name) {
		return Base64.encode(digest.digest(name.getBytes())).replace('+', '-').replace('/', '_');
	}

}
