
package net.sourceforge.filebot.web;


import static net.sourceforge.filebot.web.WebRequest.getHtmlDocument;
import static net.sourceforge.tuned.XPathUtilities.getAttribute;
import static net.sourceforge.tuned.XPathUtilities.selectNodes;
import static net.sourceforge.tuned.XPathUtilities.selectString;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Icon;

import net.sourceforge.filebot.ResourceManager;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;


public class IMDbClient implements EpisodeListClient {
	
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
	public boolean hasSingleSeasonSupport() {
		return true;
	}
	

	@Override
	public List<SearchResult> search(String query) throws IOException, SAXException {
		
		URL searchUrl = new URL("http", host, "/find?s=tt&q=" + URLEncoder.encode(query, "UTF-8"));
		
		Document dom = getHtmlDocument(openConnection(searchUrl));
		
		List<Node> nodes = selectNodes("//TABLE//A[following-sibling::SMALL[contains(.,'TV series')]]", dom);
		
		List<SearchResult> results = new ArrayList<SearchResult>(nodes.size());
		
		for (Node node : nodes) {
			String name = removeQuotationMarks(node.getTextContent().trim());
			String year = node.getNextSibling().getTextContent().trim();
			String href = getAttribute("href", node);
			
			String nameAndYear = String.format("%s %s", name, year).trim();
			int imdbId = new Scanner(href).useDelimiter("\\D+").nextInt();
			
			results.add(new MovieDescriptor(nameAndYear, imdbId));
		}
		
		return results;
	}
	

	@Override
	public List<Episode> getEpisodeList(SearchResult searchResult) throws IOException, SAXException {
		Document dom = getHtmlDocument(openConnection(getEpisodeListLink(searchResult).toURL()));
		
		String seriesName = removeQuotationMarks(selectString("//H1/A", dom));
		
		List<Node> nodes = selectNodes("//TABLE//H3/A[preceding-sibling::text()]", dom);
		
		List<Episode> episodes = new ArrayList<Episode>(nodes.size());
		
		for (Node node : nodes) {
			String title = node.getTextContent().trim();
			
			Scanner numberScanner = new Scanner(node.getPreviousSibling().getTextContent()).useDelimiter("\\D+");
			String season = numberScanner.next();
			String episode = numberScanner.next();
			
			episodes.add(new Episode(seriesName, season, episode, title));
		}
		
		return episodes;
	}
	

	@Override
	public List<Episode> getEpisodeList(SearchResult searchResult, int season) throws Exception {
		
		List<Episode> episodes = new ArrayList<Episode>(25);
		
		// remember max. season, so we can throw a proper exception, in case an illegal season number was requested
		int maxSeason = 0;
		
		// filter given season from all seasons
		for (Episode episode : getEpisodeList(searchResult)) {
			try {
				int seasonNumber = Integer.parseInt(episode.getSeasonNumber());
				
				if (season == seasonNumber) {
					episodes.add(episode);
				}
				
				if (seasonNumber > maxSeason) {
					maxSeason = seasonNumber;
				}
			} catch (NumberFormatException e) {
				Logger.getLogger(getClass().getName()).log(Level.WARNING, "Illegal season number", e);
			}
		}
		
		if (episodes.isEmpty()) {
			throw new SeasonOutOfBoundsException(searchResult.getName(), season, maxSeason);
		}
		
		return episodes;
	}
	

	protected URLConnection openConnection(URL url) throws IOException {
		URLConnection connection = url.openConnection();
		
		// IMDb refuses default user agent (Java/1.6.0_12)
		connection.addRequestProperty("User-Agent", "Scraper");
		
		return connection;
	}
	

	protected String removeQuotationMarks(String name) {
		return name.replaceAll("^\"|\"$", "");
	}
	

	@Override
	public URI getEpisodeListLink(SearchResult searchResult) {
		return URI.create("http://" + host + String.format("/title/tt%07d/episodes", ((MovieDescriptor) searchResult).getImdbId()));
	}
	

	@Override
	public URI getEpisodeListLink(SearchResult searchResult, int season) {
		return null;
	}
	
}
