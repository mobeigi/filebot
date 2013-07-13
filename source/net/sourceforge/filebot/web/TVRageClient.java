
package net.sourceforge.filebot.web;


import static net.sourceforge.filebot.web.EpisodeUtilities.*;
import static net.sourceforge.filebot.web.WebRequest.*;
import static net.sourceforge.tuned.XPathUtilities.*;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.Icon;

import net.sourceforge.filebot.Cache;
import net.sourceforge.filebot.ResourceManager;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;


public class TVRageClient extends AbstractEpisodeListProvider {
	
	private final String host = "services.tvrage.com";
	
	
	@Override
	public String getName() {
		return "TVRage";
	}
	
	
	@Override
	public Icon getIcon() {
		return ResourceManager.getIcon("search.tvrage");
	}
	
	
	@Override
	public ResultCache getCache() {
		return new ResultCache(host, Cache.getCache("web-datasource"));
	}
	
	
	@Override
	public List<SearchResult> fetchSearchResult(String query, Locale locale) throws IOException, SAXException {
		URL searchUrl = new URL("http", host, "/feeds/full_search.php?show=" + encode(query, true));
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
	public List<Episode> fetchEpisodeList(SearchResult searchResult, SortOrder sortOrder, Locale locale) throws IOException, SAXException {
		TVRageSearchResult series = (TVRageSearchResult) searchResult;
		
		URL episodeListUrl = new URL("http", host, "/feeds/full_show_info.php?sid=" + series.getSeriesId());
		Document dom = getDocument(episodeListUrl);
		
		String seriesName = selectString("Show/name", dom);
		Date seriesStartDate = Date.parse(selectString("Show/started", dom), "MMM/dd/yyyy");
		
		List<Episode> episodes = new ArrayList<Episode>(25);
		List<Episode> specials = new ArrayList<Episode>(5);
		
		// episodes and specials
		for (Node node : selectNodes("//episode", dom)) {
			String title = getTextContent("title", node);
			Integer episodeNumber = getIntegerContent("seasonnum", node);
			String seasonIdentifier = getAttribute("no", node.getParentNode());
			Integer seasonNumber = seasonIdentifier == null ? null : new Integer(seasonIdentifier);
			Date airdate = Date.parse(getTextContent("airdate", node), "yyyy-MM-dd");
			
			// check if we have season and episode number, if not it must be a special episode
			if (episodeNumber == null || seasonNumber == null) {
				// handle as special episode
				seasonNumber = getIntegerContent("season", node);
				int specialNumber = filterBySeason(specials, seasonNumber).size() + 1;
				specials.add(new Episode(seriesName, seriesStartDate, seasonNumber, null, title, null, specialNumber, airdate, searchResult));
			} else {
				// handle as normal episode
				if (sortOrder == SortOrder.Absolute) {
					episodeNumber = getIntegerContent("epnum", node);
					seasonNumber = null;
				}
				
				episodes.add(new Episode(seriesName, seriesStartDate, seasonNumber, episodeNumber, title, null, null, airdate, searchResult));
			}
		}
		
		// add specials at the end
		episodes.addAll(specials);
		
		return episodes;
	}
	
	
	@Override
	public URI getEpisodeListLink(SearchResult searchResult) {
		return URI.create(((TVRageSearchResult) searchResult).getLink() + "/episode_list/all");
	}
	
}
