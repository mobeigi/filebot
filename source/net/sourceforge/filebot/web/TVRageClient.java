
package net.sourceforge.filebot.web;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.tuned.FunctionIterator;
import net.sourceforge.tuned.ProgressIterator;
import net.sourceforge.tuned.XPathUtil;
import net.sourceforge.tuned.FunctionIterator.Function;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;


public class TVRageClient extends EpisodeListClient {
	
	private final SearchResultCache cache = new SearchResultCache();
	
	private final String host = "www.tvrage.com";
	
	
	public TVRageClient() {
		super("TVRage", ResourceManager.getIcon("search.tvrage"), true);
	}
	

	@Override
	public List<SearchResult> search(String searchterm) throws IOException, SAXException {
		if (cache.containsKey(searchterm)) {
			return Collections.singletonList(cache.get(searchterm));
		}
		
		Document dom = HtmlUtil.getHtmlDocument(getSearchUrl(searchterm));
		
		List<Node> nodes = XPathUtil.selectNodes("id('search_begin')/TABLE[1]/*/TR/TD/A[1]", dom);
		
		List<SearchResult> searchResults = new ArrayList<SearchResult>(nodes.size());
		
		for (Node node : nodes) {
			String href = XPathUtil.selectString("@href", node);
			String title = XPathUtil.selectString(".", node);
			
			try {
				URL url = new URL(href);
				
				searchResults.add(new HyperLink(title, url));
			} catch (MalformedURLException e) {
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING, "Invalid href: " + href, e);
			}
		}
		
		cache.addAll(searchResults);
		
		return searchResults;
	}
	

	@Override
	public ProgressIterator<Episode> getEpisodeList(SearchResult searchResult, int season) throws IOException, SAXException {
		
		Document dom = HtmlUtil.getHtmlDocument(getEpisodeListLink(searchResult, season));
		
		List<Node> nodes = XPathUtil.selectNodes("//TABLE[@class='b']//TR[@id='brow']", dom);
		
		return new FunctionIterator<Node, Episode>(nodes, new EpisodeFunction(searchResult, season));
	}
	
	
	private static class EpisodeFunction implements Function<Node, Episode> {
		
		private final SearchResult searchResult;
		private final int season;
		
		
		public EpisodeFunction(SearchResult searchResult, int season) {
			this.searchResult = searchResult;
			this.season = season;
		}
		

		@Override
		public Episode evaluate(Node node) {
			String seasonAndEpisodeNumber = XPathUtil.selectString("./TD[2]/A", node);
			String title = XPathUtil.selectString("./TD[5]", node);
			
			List<Node> precedings = XPathUtil.selectNodes("../preceding-sibling::TABLE", node);
			Node previousTable = precedings.get(precedings.size() - 1);
			
			String seasonHeader = XPathUtil.selectString("./TR/TD/FONT", previousTable);
			
			Matcher seasonMatcher = Pattern.compile("Season (\\d+)").matcher(seasonHeader);
			
			if (seasonMatcher.matches()) {
				if ((season == 0) || (season == Integer.parseInt(seasonMatcher.group(1)))) {
					Matcher saeMatcher = Pattern.compile("(\\d+)x(\\d+)").matcher(seasonAndEpisodeNumber);
					
					String seasonNumber = null;
					String episodeNumber = null;
					
					if (saeMatcher.matches()) {
						seasonNumber = saeMatcher.group(1);
						episodeNumber = saeMatcher.group(2);
					} else {
						episodeNumber = seasonAndEpisodeNumber;
					}
					
					return new Episode(searchResult.getName(), seasonNumber, episodeNumber, title);
				}
			}
			
			return null;
		}
	}
	
	
	@Override
	public URI getEpisodeListLink(SearchResult searchResult, int season) {
		URI baseUri = ((HyperLink) searchResult).getUri();
		
		String seasonString = "all";
		
		if (season >= 1) {
			seasonString = Integer.toString(season);
		}
		
		String path = baseUri.getPath() + "/episode_list/" + seasonString;
		
		try {
			return new URI("http", host, path, null);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
	

	private URL getSearchUrl(String searchterm) throws UnsupportedEncodingException, MalformedURLException {
		String qs = URLEncoder.encode(searchterm, "UTF-8");
		String file = "/search.php?search=" + qs;
		return new URL("http", host, file);
	}
	
}
