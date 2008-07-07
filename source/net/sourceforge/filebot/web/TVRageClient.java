
package net.sourceforge.filebot.web;


import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.tuned.XPathUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;


public class TVRageClient extends EpisodeListClient {
	
	private final SearchResultCache searchResultCache = new SearchResultCache();
	
	private final String host = "www.tvrage.com";
	
	
	public TVRageClient() {
		super("TVRage", ResourceManager.getIcon("search.tvrage"));
	}
	

	@Override
	public boolean hasSingleSeasonSupport() {
		return true;
	}
	

	@Override
	public List<SearchResult> search(String searchterm) throws SAXException, IOException, ParserConfigurationException {
		if (searchResultCache.containsKey(searchterm)) {
			return Collections.singletonList(searchResultCache.get(searchterm));
		}
		
		String searchUri = String.format("http://" + host + "/feeds/search.php?show=" + URLEncoder.encode(searchterm, "UTF-8"));
		
		Document dom = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(searchUri);
		
		List<Node> nodes = XPathUtil.selectNodes("Results/show", dom);
		
		List<SearchResult> searchResults = new ArrayList<SearchResult>(nodes.size());
		
		for (Node node : nodes) {
			int showid = XPathUtil.selectInteger("showid", node);
			String name = XPathUtil.selectString("name", node);
			String link = XPathUtil.selectString("link", node);
			
			searchResults.add(new TVRageSearchResult(name, showid, link));
		}
		
		searchResultCache.addAll(searchResults);
		
		return searchResults;
	}
	

	private EpisodeListFeed getEpisodeListFeed(SearchResult searchResult) throws SAXException, IOException, ParserConfigurationException {
		int showId = ((TVRageSearchResult) searchResult).getShowId();
		String episodeListUri = String.format("http://" + host + "/feeds/episode_list.php?sid=" + showId);
		
		Document dom = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(episodeListUri);
		
		return new EpisodeListFeed(dom);
	}
	

	@Override
	public List<Episode> getEpisodeList(SearchResult searchResult) throws Exception {
		return getEpisodeListFeed(searchResult).getEpisodeList();
	}
	

	@Override
	public List<Episode> getEpisodeList(SearchResult searchResult, int season) throws IOException, SAXException, ParserConfigurationException {
		return getEpisodeListFeed(searchResult).getEpisodeList(season);
	}
	

	@Override
	public URI getEpisodeListLink(SearchResult searchResult) {
		return getEpisodeListLink(searchResult, "all");
	}
	

	@Override
	public URI getEpisodeListLink(SearchResult searchResult, int season) {
		return getEpisodeListLink(searchResult, Integer.toString(season));
	}
	

	private URI getEpisodeListLink(SearchResult searchResult, String seasonString) {
		String base = ((TVRageSearchResult) searchResult).getLink();
		
		return URI.create(base + "/episode_list/" + seasonString);
	}
	
	
	protected static class TVRageSearchResult extends SearchResult {
		
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
	

	private static class EpisodeListFeed {
		
		private final String name;
		
		private final int totalSeasons;
		
		private final Node episodeListNode;
		
		
		public EpisodeListFeed(Document dom) {
			name = XPathUtil.selectString("Show/name", dom);
			totalSeasons = XPathUtil.selectInteger("Show/totalseasons", dom);
			
			episodeListNode = XPathUtil.selectNode("Show/Episodelist", dom);
		}
		

		public String getName() {
			return name;
		}
		

		public int getTotalSeasons() {
			return totalSeasons;
		}
		

		public List<Episode> getEpisodeList() {
			List<Episode> episodes = new ArrayList<Episode>(150);
			
			for (int i = 0; i <= getTotalSeasons(); i++) {
				episodes.addAll(getEpisodeList(i));
			}
			
			return episodes;
		}
		

		public List<Episode> getEpisodeList(int season) {
			if (season > getTotalSeasons() || season < 0)
				throw new IllegalArgumentException(String.format("%s only has %d seasons", getName(), getTotalSeasons()));
			
			String seasonString = Integer.toString(season);
			List<Node> nodes = XPathUtil.selectNodes("Season" + seasonString + "/episode", episodeListNode);
			
			List<Episode> episodes = new ArrayList<Episode>(nodes.size());
			
			for (Node node : nodes) {
				String title = XPathUtil.selectString("title", node);
				String episodeNumber = XPathUtil.selectString("seasonnum", node);
				
				episodes.add(new Episode(getName(), seasonString, episodeNumber, title));
			}
			
			return episodes;
		}
		
	}
	
}
