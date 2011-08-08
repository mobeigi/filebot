
package net.sourceforge.filebot.web;


import static net.sourceforge.filebot.web.EpisodeListUtilities.*;
import static net.sourceforge.filebot.web.WebRequest.*;
import static net.sourceforge.tuned.XPathUtilities.*;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.Icon;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import net.sourceforge.filebot.ResourceManager;


public class TVRageClient extends AbstractEpisodeListProvider {
	
	private static final String host = "services.tvrage.com";
	

	@Override
	public String getName() {
		return "TVRage";
	}
	

	@Override
	public Icon getIcon() {
		return ResourceManager.getIcon("search.tvrage");
	}
	

	@Override
	public List<SearchResult> search(String query, Locale locale) throws IOException, SAXException {
		
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
	

	@Override
	public List<Episode> getEpisodeList(SearchResult searchResult, Locale locale) throws IOException, SAXException {
		int showId = ((TVRageSearchResult) searchResult).getShowId();
		
		URL episodeListUrl = new URL("http", host, "/feeds/episode_list.php?sid=" + showId);
		
		Document dom = getDocument(episodeListUrl);
		
		String seriesName = selectString("Show/name", dom);
		
		List<Episode> episodes = new ArrayList<Episode>(25);
		List<Episode> specials = new ArrayList<Episode>(5);
		
		// episodes and specials
		for (Node node : selectNodes("//episode", dom)) {
			String title = getTextContent("title", node);
			Integer episodeNumber = getIntegerContent("seasonnum", node);
			Integer absoluteNumber = getIntegerContent("epnum", node);
			String seasonIdentifier = getAttribute("no", node.getParentNode());
			Integer seasonNumber = seasonIdentifier == null ? null : new Integer(seasonIdentifier);
			Date airdate = Date.parse(getTextContent("airdate", node), "yyyy-MM-dd");
			
			// check if we have season and episode number, if not it must be a special episode
			if (episodeNumber == null || seasonNumber == null) {
				// handle as special episode
				seasonNumber = getIntegerContent("season", node);
				int specialNumber = filterBySeason(specials, seasonNumber).size() + 1;
				specials.add(new Episode(seriesName, seasonNumber, null, title, null, specialNumber, airdate));
			} else {
				// handle as normal episode
				episodes.add(new Episode(seriesName, seasonNumber, episodeNumber, title, absoluteNumber, null, airdate));
			}
		}
		
		// add specials at the end
		episodes.addAll(specials);
		
		return episodes;
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
