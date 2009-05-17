
package net.sourceforge.filebot.web;


import static net.sourceforge.filebot.web.WebRequest.*;
import static net.sourceforge.tuned.XPathUtilities.*;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
				URL episodeListingUrl = new URL(href.replaceAll("summary\\.html\\?.*", "episode.html"));
				
				searchResults.add(new HyperLink(title, episodeListingUrl));
			} catch (Exception e) {
				Logger.getLogger(getClass().getName()).log(Level.WARNING, "Invalid href: " + href, e);
			}
		}
		
		return searchResults;
	}
	

	@Override
	public List<Episode> getEpisodeList(final SearchResult searchResult) throws Exception {
		
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
				// season used in anonymous class
				final int season = i;
				
				futures.add(executor.submit(new Callable<List<Episode>>() {
					
					@Override
					public List<Episode> call() throws Exception {
						return getEpisodeList(searchResult, season);
					}
				}));
			}
			
			// shutdown after all tasks are done
			executor.shutdown();
		}
		
		List<Episode> episodes = new ArrayList<Episode>(25 * seasonCount);
		
		// get episode list from season 1 document
		episodes.addAll(getEpisodeList(searchResult, dom));
		
		// get episodes from executor threads
		for (Future<List<Episode>> future : futures) {
			episodes.addAll(future.get());
		}
		
		return episodes;
	}
	

	@Override
	public List<Episode> getEpisodeList(SearchResult searchResult, int season) throws IOException, SAXException {
		Document dom = getHtmlDocument(getEpisodeListLink(searchResult, season).toURL());
		
		return getEpisodeList(searchResult, dom);
	}
	

	private List<Episode> getEpisodeList(SearchResult searchResult, Document dom) {
		
		List<Node> nodes = selectNodes("id('episode_guide_list')//*[@class='info']", dom);
		
		Pattern episodePattern = Pattern.compile("Season (\\d+). Episode (\\d+)");
		Pattern specialPattern = Pattern.compile("Special. Season (\\d+)");
		
		List<Episode> episodes = new ArrayList<Episode>(nodes.size());
		
		for (Node node : nodes) {
			String title = selectString("./H3/A/text()", node);
			String meta = selectString("./*[@class='meta']", node).replaceAll("\\p{Space}+", " ");
			
			String season = null;
			String episode = null;
			
			Matcher matcher;
			
			if ((matcher = episodePattern.matcher(meta)).find()) {
				// matches episode
				season = matcher.group(1);
				episode = matcher.group(2);
			} else if ((matcher = specialPattern.matcher(meta)).find()) {
				// matches special 
				season = matcher.group(1);
				episode = "Special";
			} else {
				// no episode match
				continue;
			}
			
			episodes.add(new Episode(searchResult.getName(), season, episode, title));
		}
		
		return episodes;
	}
	

	@Override
	public URI getEpisodeListLink(SearchResult searchResult) {
		return getEpisodeListLink(searchResult, "All");
	}
	

	@Override
	public URI getEpisodeListLink(SearchResult searchResult, int season) {
		return getEpisodeListLink(searchResult, Integer.toString(season));
	}
	

	public URI getEpisodeListLink(SearchResult searchResult, String season) {
		URL episodeGuide = ((HyperLink) searchResult).getURL();
		
		return URI.create(episodeGuide + "?season=" + season);
	}
}
