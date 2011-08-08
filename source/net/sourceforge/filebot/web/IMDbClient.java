
package net.sourceforge.filebot.web;


import static net.sourceforge.filebot.web.WebRequest.*;
import static net.sourceforge.tuned.XPathUtilities.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Icon;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import net.sourceforge.filebot.ResourceManager;


public class IMDbClient extends AbstractEpisodeListProvider {
	
	private static final String host = "www.imdb.com";
	

	@Override
	public String getName() {
		return "IMDb";
	}
	

	@Override
	public Icon getIcon() {
		return ResourceManager.getIcon("search.imdb");
	}
	

	@Override
	public List<SearchResult> search(String query, Locale locale) throws IOException, SAXException {
		
		URL searchUrl = new URL("http", host, "/find?s=tt&q=" + URLEncoder.encode(query, "UTF-8"));
		
		Document dom = getHtmlDocument(openConnection(searchUrl));
		
		List<Node> nodes = selectNodes("//TABLE//A[following-sibling::SMALL[contains(.,'series')]]", dom);
		
		List<SearchResult> results = new ArrayList<SearchResult>(nodes.size());
		
		for (Node node : nodes) {
			String name = normalizeName(node.getTextContent().trim());
			String year = node.getNextSibling().getTextContent().trim().replaceAll("\\D+", ""); // remove non-number characters
			String href = getAttribute("href", node);
			
			results.add(new MovieDescriptor(name, Integer.parseInt(year), getImdbId(href)));
		}
		
		// we might have been redirected to the movie page
		if (results.isEmpty()) {
			try {
				String name = normalizeName(selectString("//H1/text()", dom));
				String year = new Scanner(selectString("//H1//SPAN", dom)).useDelimiter("\\D+").next();
				String url = selectString("//LINK[@rel='canonical']/@href", dom);
				
				results.add(new MovieDescriptor(name, Integer.parseInt(year), getImdbId(url)));
			} catch (Exception e) {
				// ignore, we probably got redirected to an error page
			}
		}
		
		return results;
	}
	

	@Override
	public List<Episode> getEpisodeList(SearchResult searchResult, Locale locale) throws IOException, SAXException {
		Document dom = getHtmlDocument(openConnection(getEpisodeListLink(searchResult).toURL()));
		
		String seriesName = normalizeName(selectString("//H1/A", dom));
		
		List<Node> nodes = selectNodes("//TABLE//H3/A[preceding-sibling::text()]", dom);
		
		List<Episode> episodes = new ArrayList<Episode>(nodes.size());
		
		for (Node node : nodes) {
			String title = getTextContent(node);
			
			Scanner numberScanner = new Scanner(node.getPreviousSibling().getTextContent()).useDelimiter("\\D+");
			Integer season = numberScanner.nextInt();
			Integer episode = numberScanner.nextInt();
			
			// e.g. 20 May 2003
			String airdate = selectString("./following::STRONG", node);
			
			episodes.add(new Episode(seriesName, season, episode, title, null, null, Date.parse(airdate, "dd MMMMM yyyyy")));
		}
		
		return episodes;
	}
	

	protected URLConnection openConnection(URL url) throws IOException {
		URLConnection connection = url.openConnection();
		
		// IMDb refuses default user agent (Java/1.6.0_12)
		connection.addRequestProperty("User-Agent", "Scraper");
		
		return connection;
	}
	

	protected String normalizeName(String name) {
		// remove quotation marks
		return name.replaceAll("\"", "");
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
	public URI getEpisodeListLink(SearchResult searchResult) {
		return getEpisodeListLink(searchResult, 0);
	}
	

	@Override
	public URI getEpisodeListLink(SearchResult searchResult, int season) {
		try {
			return new URI("http", host, String.format("/title/tt%07d/episodes", ((MovieDescriptor) searchResult).getImdbId()), season > 0 ? String.format("season-%d", season) : null);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
}
