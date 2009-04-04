
package net.sourceforge.filebot.web;


import static net.sourceforge.filebot.web.WebRequest.getHtmlDocument;
import static net.sourceforge.tuned.XPathUtilities.getAttribute;
import static net.sourceforge.tuned.XPathUtilities.getTextContent;
import static net.sourceforge.tuned.XPathUtilities.selectNodes;
import static net.sourceforge.tuned.XPathUtilities.selectString;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Icon;

import net.sourceforge.filebot.ResourceManager;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;


public class TVDotComClient implements EpisodeListProvider {
	
	private static final String host = "www.tv.com";
	
	
	@Override
	public String getName() {
		return "TV.com";
	}
	

	@Override
	public Icon getIcon() {
		return ResourceManager.getIcon("search.tvdotcom");
	}
	

	@Override
	public boolean hasSingleSeasonSupport() {
		return true;
	}
	

	@Override
	public List<SearchResult> search(String query) throws IOException, SAXException {
		
		// use ajax search request, because we don't need the whole search result page
		URL searchUrl = new URL("http", host, "/search.php?type=Search&stype=ajax_search&search_type=program&qs=" + URLEncoder.encode(query, "UTF-8"));
		
		Document dom = getHtmlDocument(searchUrl);
		
		List<Node> nodes = selectNodes("//*[@class='title']//descendant-or-self::A", dom);
		
		List<SearchResult> searchResults = new ArrayList<SearchResult>(nodes.size());
		
		for (Node node : nodes) {
			String title = getTextContent(node);
			String href = getAttribute("href", node);
			
			try {
				URL episodeListingUrl = new URL(href.replaceFirst("summary.html\\?.*", "episode_listings.html"));
				
				searchResults.add(new HyperLink(title, episodeListingUrl));
			} catch (Exception e) {
				Logger.getLogger(getClass().getName()).log(Level.WARNING, "Invalid href: " + href, e);
			}
		}
		
		return searchResults;
	}
	

	@Override
	public List<Episode> getEpisodeList(SearchResult searchResult) throws Exception {
		
		// get document for season 1
		Document dom = getHtmlDocument(getEpisodeListLink(searchResult, 1).toURL());
		
		// seasons are ordered in reverse, first element is latest season
		String latestSeasonString = selectString("id('episode_list_header')//*[contains(@class, 'number')]", dom);
		
		if (latestSeasonString.isEmpty()) {
			// assume single season series
			latestSeasonString = "1";
		}
		
		// strip unexpected characters from season string (e.g. "7...");
		int seasonCount = Integer.valueOf(latestSeasonString.replaceAll("\\D+", ""));
		
		// we're going to fetch the episode list for each season on multiple threads
		List<Future<List<Episode>>> futures = new ArrayList<Future<List<Episode>>>(seasonCount);
		
		if (seasonCount > 1) {
			// max. 12 threads so we don't get too many concurrent connections
			ExecutorService executor = Executors.newFixedThreadPool(Math.min(seasonCount - 1, 12));
			
			// we already have the document for season 1, start with season 2
			for (int i = 2; i <= seasonCount; i++) {
				futures.add(executor.submit(new GetEpisodeList(searchResult, i)));
			}
			
			// shutdown after all tasks are done
			executor.shutdown();
		}
		
		List<Episode> episodes = new ArrayList<Episode>(25 * seasonCount);
		
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
		
		Document dom = getHtmlDocument(getEpisodeListLink(searchResult, season).toURL());
		
		return getEpisodeList(searchResult, season, dom);
	}
	

	private List<Episode> getEpisodeList(SearchResult searchResult, int season, Document dom) {
		
		List<Node> nodes = selectNodes("id('episode_listing')//*[@class='episode']", dom);
		
		Integer episodeOffset = null;
		
		List<Episode> episodes = new ArrayList<Episode>(nodes.size());
		
		for (Node node : nodes) {
			String episodeNumber = selectString("./*[@class='number']", node);
			String title = selectString("./*[@class='title']", node);
			String seasonNumber = String.valueOf(season);
			
			try {
				// convert the absolute episode number to the season episode number
				int n = Integer.parseInt(episodeNumber);
				
				if (episodeOffset == null)
					episodeOffset = (n <= 1) ? 0 : n - 1;
				
				episodeNumber = String.valueOf(n - episodeOffset);
			} catch (NumberFormatException e) {
				// episode may be "Pilot", "Special", "TV Movie" ...
				seasonNumber = null;
			}
			
			episodes.add(new Episode(searchResult.getName(), seasonNumber, episodeNumber, title));
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
