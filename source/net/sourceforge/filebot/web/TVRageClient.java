
package net.sourceforge.filebot.web;


import static net.sourceforge.filebot.web.EpisodeListUtilities.filterBySeason;
import static net.sourceforge.filebot.web.EpisodeListUtilities.getLastSeason;
import static net.sourceforge.filebot.web.WebRequest.getDocument;
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

import javax.swing.Icon;
import javax.xml.parsers.ParserConfigurationException;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sourceforge.filebot.ResourceManager;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;


public class TVRageClient implements EpisodeListProvider {
	
	private static final String host = "www.tvrage.com";
	
	private final Cache cache = CacheManager.getInstance().getCache("web");
	
	
	@Override
	public String getName() {
		return "TVRage";
	}
	

	@Override
	public Icon getIcon() {
		return ResourceManager.getIcon("search.tvrage");
	}
	

	@Override
	public boolean hasSingleSeasonSupport() {
		return true;
	}
	

	@Override
	public List<SearchResult> search(String query) throws SAXException, IOException, ParserConfigurationException {
		
		URL searchUrl = new URL("http", host, "/feeds/full_search.php?show=" + URLEncoder.encode(query, "UTF-8"));
		
		Document dom = getDocument(searchUrl);
		
		List<Node> nodes = selectNodes("Results/show", dom);
		
		List<SearchResult> searchResults = new ArrayList<SearchResult>(nodes.size());
		
		for (Node node : nodes) {
			int showid = Integer.parseInt(getTextContent("showid", node));
			String name = getTextContent("name", node);
			String link = getTextContent("link", node);
			
			searchResults.add(new TVRageSearchResult(name, showid, link));
		}
		
		return searchResults;
	}
	

	@SuppressWarnings("unchecked")
	@Override
	public List<Episode> getEpisodeList(SearchResult searchResult) throws IOException, SAXException, ParserConfigurationException {
		int showId = ((TVRageSearchResult) searchResult).getShowId();
		
		URL episodeListUrl = new URL("http", host, "/feeds/episode_list.php?sid=" + showId);
		
		// try to load from cache
		Element cacheEntry = cache.get(episodeListUrl.toString());
		
		if (cacheEntry != null) {
			return (List<Episode>) cacheEntry.getValue();
		}
		
		Document dom = getDocument(episodeListUrl);
		
		String seriesName = selectString("Show/name", dom);
		
		List<Episode> episodes = new ArrayList<Episode>(25);
		
		// episodes and specials
		for (Node node : selectNodes("//episode", dom)) {
			String title = getTextContent("title", node);
			String episodeNumber = getTextContent("seasonnum", node);
			String seasonNumber = getAttribute("no", node.getParentNode());
			
			// check if we have season and episode number, if not it must be a special episode
			if (episodeNumber == null || seasonNumber == null) {
				episodeNumber = "Special";
				seasonNumber = getTextContent("season", node);
			}
			
			episodes.add(new Episode(seriesName, seasonNumber, episodeNumber, title));
		}
		
		// populate cache
		cache.put(new Element(episodeListUrl.toString(), episodes));
		
		return episodes;
	}
	

	@Override
	public List<Episode> getEpisodeList(SearchResult searchResult, int season) throws IOException, SAXException, ParserConfigurationException {
		List<Episode> all = getEpisodeList(searchResult);
		List<Episode> eps = filterBySeason(all, season);
		
		if (eps.isEmpty()) {
			throw new SeasonOutOfBoundsException(searchResult.getName(), season, getLastSeason(all));
		}
		
		return eps;
	}
	

	@Override
	public URI getEpisodeListLink(SearchResult searchResult) {
		return getEpisodeListLink(searchResult, "all");
	}
	

	@Override
	public URI getEpisodeListLink(SearchResult searchResult, int season) {
		return getEpisodeListLink(searchResult, String.valueOf(season));
	}
	

	private URI getEpisodeListLink(SearchResult searchResult, String seasonString) {
		String base = ((TVRageSearchResult) searchResult).getLink();
		
		return URI.create(base + "/episode_list/" + seasonString);
	}
	
	
	public static class TVRageSearchResult extends SearchResult {
		
		private final int showId;
		private final String link;
		
		
		public TVRageSearchResult(String name, int showId, String link) {
			super(name);
			this.showId = showId;
			this.link = link;
		}
		

		public int getShowId() {
			return showId;
		}
		

		public String getLink() {
			return link;
		}
		
	}
	
}
