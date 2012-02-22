
package net.sourceforge.filebot.web;


import static net.sourceforge.filebot.web.WebRequest.*;
import static net.sourceforge.tuned.XPathUtilities.*;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Icon;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import net.sourceforge.filebot.ResourceManager;


public class IMDbClient implements MovieIdentificationService {
	
	private final String host = "akas.imdb.com";
	
	
	@Override
	public String getName() {
		return "IMDb";
	}
	
	
	@Override
	public Icon getIcon() {
		return ResourceManager.getIcon("search.imdb");
	}
	
	
	protected int getImdbId(String link) {
		Matcher matcher = Pattern.compile("tt(\\d{7})").matcher(link);
		
		if (matcher.find()) {
			return Integer.parseInt(matcher.group(1));
		}
		
		// pattern not found
		throw new IllegalArgumentException(String.format("Cannot find imdb id: %s", link));
	}
	
	
	@Override
	public List<Movie> searchMovie(String query, Locale locale) throws Exception {
		Document dom = parsePage(new URL("http", host, "/find?s=tt&q=" + encode(query)));
		
		// select movie links followed by year in parenthesis
		List<Node> nodes = selectNodes("//TABLE//A[substring-after(substring-before(following::text(),')'),'(')]", dom);
		List<Movie> results = new ArrayList<Movie>(nodes.size());
		
		for (Node node : nodes) {
			try {
				String name = node.getTextContent().trim();
				if (name.startsWith("\""))
					continue;
				
				String year = node.getNextSibling().getTextContent().replaceAll("[\\p{Punct}\\p{Space}]+", ""); // remove non-number characters
				String href = getAttribute("href", node);
				
				results.add(new Movie(name, Integer.parseInt(year), getImdbId(href)));
			} catch (Exception e) {
				// ignore illegal movies (TV Shows, Videos, Video Games, etc)
			}
		}
		
		// we might have been redirected to the movie page
		if (results.isEmpty()) {
			Movie movie = scrapeMovie(dom);
			if (movie != null) {
				results.add(movie);
			}
		}
		
		return results;
	}
	
	
	protected Movie scrapeMovie(Document dom) {
		try {
			String name = selectString("//H1/A/text()", dom);
			String year = new Scanner(selectString("//H1/A/following::A/text()", dom)).useDelimiter("\\D+").next();
			String url = selectString("//H1/A/@href", dom);
			return new Movie(name, Pattern.matches("\\d{4}", year) ? Integer.parseInt(year) : -1, getImdbId(url));
		} catch (Exception e) {
			// ignore, we probably got redirected to an error page
			return null;
		}
	}
	
	
	@Override
	public Movie getMovieDescriptor(int imdbid, Locale locale) throws Exception {
		return scrapeMovie(parsePage(new URL("http", host, String.format("/title/tt%07d/releaseinfo", imdbid))));
	}
	
	
	protected Document parsePage(URL url) throws IOException, SAXException {
		CachedPage page = new CachedPage(url) {
			
			@Override
			protected Reader openConnection(URL url) throws IOException {
				URLConnection connection = url.openConnection();
				
				// IMDb refuses default user agent (Java/1.6.0_12)
				connection.addRequestProperty("User-Agent", "Mozilla");
				
				return getReader(connection);
			}
		};
		
		return getHtmlDocument(page.get());
	}
	
	
	@Override
	public Map<File, Movie> getMovieDescriptors(Collection<File> movieFiles, Locale locale) throws Exception {
		throw new UnsupportedOperationException();
	}
	
}
