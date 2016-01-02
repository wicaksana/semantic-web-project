package semweb;

import com.sun.org.apache.xml.internal.security.utils.Base64;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

/**
 * Crawler to generate RDF from website
 * 
 * @author Iwan Timmer
 */
public class Crawler {
	
	private final static String WOLFF_BASE = "http://www.wolff.nl";
	private final static String WOLFF_URI = WOLFF_BASE + "/";
	
	private final MessageDigest digest;
	
	private final Model model;
	private final Property name, actor, releaseDate, genre, duration, language, director, distributor, link;
	
	/**
	 * Create new crawler
	 * @throws IOException
	 * @throws NoSuchAlgorithmException 
	 */
	public Crawler() throws IOException, NoSuchAlgorithmException {
		digest = MessageDigest.getInstance("SHA1");
		
		model = ModelFactory.createDefaultModel();
		model.setNsPrefix("wolff", WOLFF_URI);
		
		name = model.createProperty(WOLFF_URI, "name");
		actor = model.createProperty(WOLFF_URI, "actor");
		releaseDate = model.createProperty(WOLFF_URI, "releaseDate");
		genre = model.createProperty(WOLFF_URI, "genre");
		duration = model.createProperty(WOLFF_URI, "duration");
		language = model.createProperty(WOLFF_URI, "language");
		director = model.createProperty(WOLFF_URI, "director");
		distributor = model.createProperty(WOLFF_URI, "distributor");
		link = model.createProperty(WOLFF_URI, "link");
	}
	
	/**
	 * Parse index page and load all different movie pages
	 * @throws IOException 
	 */
	public void parseIndex() throws IOException {
		Document doc = Jsoup.connect(WOLFF_BASE + "/bioscopen/cinestar/").get();
		Elements movies = doc.select(".table_agenda td[width=245] a");
		
		Set<String> urls = new HashSet<>();
		for (Element movie:movies) {
			urls.add(movie.attr("href"));
		}
		
		for (String url:urls) {
			parseMovie(WOLFF_BASE + url);
		}
	}
	
	/**
	 * Get last child node from element
	 * @param element
	 * @return last node
	 */
	private Node last(Element element) {
		List<Node> nodes = element.childNodes();
		return nodes.get(nodes.size()-1);
	}
	
	/**
	 * Encode string to create random url per name
	 * @param name
	 * @return base64 encoded hash
	 */
	private String encode(String name) {
		return Base64.encode(digest.digest(name.getBytes())).replace('+', '-').replace('/', '_');
	}
	
	/**
	 * Parse moviepage and create model
	 * @param url of movie
	 * @throws IOException 
	 */
	private void parseMovie(String url) throws IOException {
		Document doc = Jsoup.connect(url).get();
		
		Resource movie = model.createResource(url);
		movie.addProperty(name, doc.select("#wrapper_left h2").text().trim());		

		Elements linkElement = doc.select(".link_official");
		if (linkElement.size() > 0)
			movie.addProperty(link, linkElement.attr("href"));
		
		//Parse sidebar information
		Elements sidebar = doc.select(".sidebar_container").get(1).select(".table_view tr td");
		for (Element element:sidebar) {
			switch (element.select("strong").text()) {
				case "Acteurs":
					String[] actors = element.text().substring(8).split(", "); //Remove "Acteurs" from string
					for (String actorName:actors) {
						Resource person = model.createResource(WOLFF_BASE + "/person#" + encode(actorName.trim()));
						person.addProperty(name, actorName.trim());
						movie.addProperty(actor, person);
					}

					break;
				case "In de bioscoop sinds":
					movie.addProperty(releaseDate, last(element).toString().trim());			
					break;
				case "Genre":
					String[] genres = last(element).toString().split(", ");
					for (String genreName:genres)
						movie.addProperty(genre, genreName.trim());
					break;
				case "Speelduur":
					movie.addProperty(duration, last(element).toString().trim());
					break;
				case "Taal":
					movie.addProperty(language, last(element).toString().trim());	
					break;
				case "Regie":
					String nameString = element.text().substring(6).toString().trim(); //Remove "Regie" from string
					Resource person = model.createResource(WOLFF_BASE + "/person#" + encode(nameString));
					person.addProperty(name, nameString);
					movie.addProperty(director, person);
					break;
				case "Distributeur":
					Node distributorLink = last(element);
					Resource distributorResource;
					if (distributorLink instanceof Element) {
						distributorResource = model.createResource(WOLFF_BASE + "/distributor#" + encode(((Element) distributorLink).text()));
						distributorResource.addProperty(link, distributorLink.attr("href"));
						distributorResource.addProperty(name, ((Element) distributorLink).text());
						movie.addProperty(distributor, distributorResource);
					} else {
						distributorResource = model.createResource(WOLFF_URI + "/distributor#" + encode(distributorLink.toString()));
						distributorResource.addProperty(name, distributorLink.toString());
					}
						
					movie.addProperty(distributor, distributorResource);
					break;
			}
		}
	}
	
	/**
	 * Save created model to file
	 * @throws FileNotFoundException 
	 */
	public void save() throws FileNotFoundException {
		model.write(new FileOutputStream("theatre.xml"), "RDF/XML");
	}

	public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
		Crawler crawler = new Crawler();
		crawler.parseIndex();
		crawler.save();
	}
	
}
