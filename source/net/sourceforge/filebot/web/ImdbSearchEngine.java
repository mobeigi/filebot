
package net.sourceforge.filebot.web;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;

import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.tuned.XPathUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;


public class ImdbSearchEngine {
	
	private String host = "www.imdb.com";
	
	
	public List<Movie> search(String searchterm) throws IOException, SAXException {
		
		Document dom = HtmlUtil.getHtmlDocument(getSearchUrl(searchterm));
		
		List<Node> nodes = XPathUtil.selectNodes("id('outerbody')//TABLE//P[position() >= 2 and position() <=3 ]//A[count(child::IMG) <= 0]/..", dom);
		
		ArrayList<Movie> movies = new ArrayList<Movie>();
		
		for (Node node : nodes) {
			try {
				movies.add(parseMovie(node));
			} catch (Exception e) {
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING, "Invalid movie node", e);
			}
		}
		
		return movies;
	}
	

	private Movie parseMovie(Node node) {
		// ignore javascript links
		Node linkNode = XPathUtil.selectFirstNode("./A[count(@onclick) <= 0]", node);
		
		String title = XPathUtil.selectString("text()", linkNode);
		String href = XPathUtil.selectString("@href", linkNode);
		
		// match /title/tt0379786/
		Matcher idMatcher = Pattern.compile(".*/tt(\\d+)/.*").matcher(href);
		Integer imdbID = null;
		
		if (idMatcher.matches()) {
			imdbID = new Integer(idMatcher.group(1));
		} else {
			throw new IllegalArgumentException("Cannot match imdb id: " + href);
		}
		
		String yearString = XPathUtil.selectString("text()[1]", node);
		
		// match (2005)
		Matcher yearMatcher = Pattern.compile(".*\\((\\d+)\\).*").matcher(yearString);
		Integer year = null;
		
		if (yearMatcher.matches()) {
			year = new Integer(yearMatcher.group(1));
		} else {
			throw new IllegalArgumentException("Cannot match year: " + yearString);
		}
		
		return new Movie(title, year, imdbID);
	}
	

	private URL getSearchUrl(String searchterm) throws UnsupportedEncodingException, MalformedURLException {
		String qs = URLEncoder.encode(searchterm, "UTF-8");
		String file = "/find?q=" + qs + ";s=tt";
		return new URL("http", host, file);
	}
	

	public ImageIcon getIcon() {
		return ResourceManager.getIcon("search.imdb");
	}
	
}
