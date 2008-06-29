
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
import net.sourceforge.tuned.DefaultProgressIterator;
import net.sourceforge.tuned.ProgressIterator;
import net.sourceforge.tuned.XPathUtil;

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
	public List<SearchResult> search(String searchterm) throws SAXException, IOException, ParserConfigurationException {
		if (cache.containsKey(searchterm)) {
			return Collections.singletonList(cache.get(searchterm));
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
		
		cache.addAll(searchResults);
		
		return searchResults;
	}
	

	@Override
	public ProgressIterator<Episode> getEpisodeList(SearchResult searchResult, int season) throws IOException, SAXException, ParserConfigurationException {
		
		int showId = ((TVRageSearchResult) searchResult).getShowId();
		String episodeListUri = String.format("http://" + host + "/feeds/episode_list.php?sid=" + showId);
		
		Document dom = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(episodeListUri);
		
		int numberOfSeasons = XPathUtil.selectInteger("Show/totalseasons", dom);
		
		if (season > numberOfSeasons)
			throw new IllegalArgumentException(String.format("%s only has %d seasons", searchResult.getName(), numberOfSeasons));
		
		Node episodeListNode = XPathUtil.selectNode("Show/Episodelist", dom);
		
		boolean allSeasons = (season == 0);
		
		List<Episode> episodes = new ArrayList<Episode>(24);
		
		for (int i = 0; i <= numberOfSeasons; i++) {
			if (i == season || allSeasons) {
				List<Node> nodes = XPathUtil.selectNodes("Season" + i + "/episode", episodeListNode);
				
				for (Node node : nodes) {
					String title = XPathUtil.selectString("title", node);
					String episodeNumber = XPathUtil.selectString("seasonnum", node);
					String seasonNumber = Integer.toString(i);
					
					episodes.add(new Episode(searchResult.getName(), seasonNumber, episodeNumber, title));
				}
			}
		}
		
		return new DefaultProgressIterator<Episode>(episodes);
	}
	

	@Override
	public URI getEpisodeListLink(SearchResult searchResult, int season) {
		String page = ((TVRageSearchResult) searchResult).getLink();
		String seasonString = (season >= 1) ? Integer.toString(season) : "all";
		
		return URI.create(page + "/episode_list/" + seasonString);
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
