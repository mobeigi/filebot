
package net.sourceforge.filebot.web;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.tuned.XPathUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;


public class TVDotComClient extends EpisodeListClient {
	
	private final SearchResultCache cache = new SearchResultCache();
	
	private final String host = "www.tv.com";
	
	
	public TVDotComClient() {
		super("TV.com", ResourceManager.getIcon("search.tvdotcom"));
	}
	

	@Override
	public boolean hasSingleSeasonSupport() {
		return true;
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
			String title = node.getTextContent();
			String href = XPathUtil.selectString("@href", node);
			
			try {
				URL episodeListingUrl = new URL(href.replaceFirst(Pattern.quote("summary.html?") + ".*", "episode_listings.html"));
				
				searchResults.add(new HyperLink(title, episodeListingUrl));
			} catch (MalformedURLException e) {
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING, "Invalid href: " + href, e);
			}
		}
		
		cache.addAll(searchResults);
		
		return searchResults;
	}
	

	@Override
	public List<Episode> getEpisodeList(SearchResult searchResult) throws Exception {
		
		// get document for season 1
		Document dom = HtmlUtil.getHtmlDocument(getEpisodeListLink(searchResult, 1));
		
		int seasonCount = XPathUtil.selectInteger("count(id('season-dropdown')//SELECT/OPTION[text() != 'All Seasons'])", dom);
		
		// we're going to fetch the episode list for each season on multiple threads
		List<Future<List<Episode>>> futures = new ArrayList<Future<List<Episode>>>(seasonCount);
		
		if (seasonCount > 1) {
			// max. 12 threads so we don't get too many concurrent downloads
			ExecutorService executor = Executors.newFixedThreadPool(Math.min(seasonCount - 1, 12));
			
			// we already have the document for season 1, start with season 2
			for (int i = 2; i <= seasonCount; i++) {
				futures.add(executor.submit(new GetEpisodeList(searchResult, i)));
			}
			
			// shutdown after all tasks are done
			executor.shutdown();
		}
		
		List<Episode> episodes = new ArrayList<Episode>(150);
		
		// get episode list from season 1 document
		episodes.addAll(getEpisodeList(searchResult, 1, dom));
		
		// get episodes from executor threads
		for (Future<List<Episode>> future : futures) {
			episodes.addAll(future.get());
		}
		
		return episodes;
	}
	

	@Override
	public List<Episode> getEpisodeList(SearchResult searchResult, int season) throws IOException, SAXException {
		
		Document dom = HtmlUtil.getHtmlDocument(getEpisodeListLink(searchResult, season));
		
		return getEpisodeList(searchResult, season, dom);
	}
	

	private List<Episode> getEpisodeList(SearchResult searchResult, int season, Document dom) {
		
		List<Node> nodes = XPathUtil.selectNodes("id('episode-listing')/DIV/TABLE/TR/TD/ancestor::TR", dom);
		
		NumberFormat numberFormat = NumberFormat.getInstance(Locale.ENGLISH);
		numberFormat.setMinimumIntegerDigits(Math.max(Integer.toString(nodes.size()).length(), 2));
		numberFormat.setGroupingUsed(false);
		
		Integer episodeOffset = null;
		
		ArrayList<Episode> episodes = new ArrayList<Episode>(nodes.size());
		
		for (Node node : nodes) {
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
			
			episodes.add(new Episode(searchResult.getName(), Integer.toString(season), episodeNumber, title));
		}
		
		return episodes;
	}
	

	@Override
	public URI getEpisodeListLink(SearchResult searchResult) {
		return getEpisodeListLink(searchResult, 0);
	}
	

	@Override
	public URI getEpisodeListLink(SearchResult searchResult, int season) {
		URL episodeListingUrl = ((HyperLink) searchResult).getURL();
		
		return URI.create(episodeListingUrl + "?season=" + season);
	}
	

	private URL getSearchUrl(String searchterm) throws UnsupportedEncodingException, MalformedURLException {
		String qs = URLEncoder.encode(searchterm, "UTF-8");
		String file = "/search.php?type=Search&stype=ajax_search&search_type=program&qs=" + qs;
		
		return new URL("http", host, file);
	}
	
	
	private class GetEpisodeList implements Callable<List<Episode>> {
		
		private final SearchResult searchResult;
		private final int season;
		
		
		public GetEpisodeList(SearchResult searchResult, int season) {
			this.searchResult = searchResult;
			this.season = season;
		}
		

		@Override
		public List<Episode> call() throws Exception {
			return getEpisodeList(searchResult, season);
		}
	}
	
}
