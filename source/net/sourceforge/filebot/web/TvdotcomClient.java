
package net.sourceforge.filebot.web;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.tuned.FunctionIterator;
import net.sourceforge.tuned.ProgressIterator;
import net.sourceforge.tuned.XPathUtil;
import net.sourceforge.tuned.FunctionIterator.Function;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;


public class TvdotcomClient extends EpisodeListClient {
	
	private final SearchResultCache cache = new SearchResultCache();
	
	private final String host = "www.tv.com";
	
	
	public TvdotcomClient() {
		super("TV.com", ResourceManager.getIcon("search.tvdotcom"), true);
	}
	

	@Override
	public List<SearchResult> search(String searchterm) throws IOException, SAXException {
		if (cache.containsKey(searchterm)) {
			return Collections.singletonList(cache.get(searchterm));
		}
		
		Document dom = HtmlUtil.getHtmlDocument(getSearchUrl(searchterm));
		
		List<Node> nodes = XPathUtil.selectNodes("id('search-results')//SPAN/A", dom);
		
		List<SearchResult> searchResults = new ArrayList<SearchResult>(nodes.size());
		
		for (Node node : nodes) {
			String category = node.getParentNode().getTextContent();
			
			// we only want search results that are shows
			if (category.toLowerCase().startsWith("show")) {
				String title = node.getTextContent();
				String href = XPathUtil.selectString("@href", node);
				
				try {
					URL url = new URL(href);
					
					searchResults.add(new HyperLink(title, url));
				} catch (MalformedURLException e) {
					Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING, "Invalid href: " + href, e);
				}
			}
		}
		
		cache.addAll(searchResults);
		
		return searchResults;
	}
	

	@Override
	public ProgressIterator<Episode> getEpisodeList(SearchResult searchResult, int season) throws IOException, SAXException {
		
		Document dom = HtmlUtil.getHtmlDocument(getEpisodeListLink(searchResult, season));
		
		List<Node> nodes = XPathUtil.selectNodes("id('episode-listing')/DIV/TABLE/TR/TD/ancestor::TR", dom);
		
		return new FunctionIterator<Node, Episode>(nodes, new EpisodeFunction(searchResult, season, nodes.size()));
	}
	
	
	private static class EpisodeFunction implements Function<Node, Episode> {
		
		private final SearchResult searchResult;
		private final NumberFormat numberFormat;
		
		private Integer episodeOffset = null;
		private String seasonString = null;
		
		
		public EpisodeFunction(SearchResult searchResult, int season, int nodeCount) {
			this.searchResult = searchResult;
			
			numberFormat = NumberFormat.getInstance(Locale.ENGLISH);
			numberFormat.setMinimumIntegerDigits(Math.max(Integer.toString(nodeCount).length(), 2));
			numberFormat.setGroupingUsed(false);
			
			if (season >= 1)
				seasonString = Integer.toString(season);
		}
		

		@Override
		public Episode evaluate(Node node) {
			String episodeNumber = XPathUtil.selectString("./TD[1]", node);
			String title = XPathUtil.selectString("./TD[2]/A", node);
			
			try {
				// format number of episode
				int n = Integer.parseInt(episodeNumber);
				
				if (episodeOffset == null)
					episodeOffset = n - 1;
				
				episodeNumber = numberFormat.format(n - episodeOffset);
			} catch (NumberFormatException e) {
				// episode number may be "Pilot", "Special", ...
			}
			
			return new Episode(searchResult.getName(), seasonString, episodeNumber, title);
		}
	}
	
	
	@Override
	public URI getEpisodeListLink(SearchResult searchResult, int season) {
		String summaryFile = null;
		
		summaryFile = ((HyperLink) searchResult).getUri().getPath();
		
		String base = summaryFile.substring(0, summaryFile.indexOf("summary.html"));
		String file = base + "episode_listings.html";
		String query = "season=" + season;
		
		try {
			return new URI("http", host, file, query, null);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
	

	private URL getSearchUrl(String searchterm) throws UnsupportedEncodingException, MalformedURLException {
		String qs = URLEncoder.encode(searchterm, "UTF-8");
		String file = "/search.php?qs=" + qs + "&type=11&stype=all";
		
		return new URL("http", host, file);
	}
	
}
