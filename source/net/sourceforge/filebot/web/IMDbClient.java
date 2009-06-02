
package net.sourceforge.filebot.web;


import static net.sourceforge.filebot.web.EpisodeListUtilities.*;
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
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Icon;

import net.sourceforge.filebot.ResourceManager;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;


public class IMDbClient implements EpisodeListProvider {
	
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
			
			results.add(new MovieDescriptor(name, Integer.parseInt(year), getImdbId(href)));
		}
		
		// we might have been redirected to the movie page
		if (results.isEmpty()) {
			String name = removeQuotationMarks(selectString("//H1/text()", dom));
			String year = selectString("//H1//A", dom);
			String url = selectString("//LINK[@rel='canonical']/@href", dom);
			
			results.add(new MovieDescriptor(name, Integer.parseInt(year), getImdbId(url)));
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
		List<Episode> all = getEpisodeList(searchResult);
		List<Episode> eps = filterBySeason(all, season);
		
		if (eps.isEmpty()) {
			throw new SeasonOutOfBoundsException(searchResult.getName(), season, getLastSeason(all));
		}
		
		return eps;
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
	

	protected int getImdbId(String link) {
		try {
			// try to extract path
			link = new URI(link).getPath();
		} catch (URISyntaxException e) {
			// cannot extract path component, just move on
		}
		
		Matcher matcher = Pattern.compile("tt(\\d{7})").matcher(link);
		
		String imdbId = null;
		
		// find last match
		while (matcher.find()) {
			imdbId = matcher.group(1);
		}
		
		if (imdbId == null)
			throw new IllegalArgumentException(String.format("Cannot find imdb id: %s", link));
		
		return Integer.parseInt(imdbId);
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
