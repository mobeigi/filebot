package net.filebot.web;

import static java.util.Collections.*;
import static net.filebot.util.XPathUtilities.*;
import static net.filebot.web.EpisodeUtilities.*;
import static net.filebot.web.WebRequest.*;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.Icon;

import net.filebot.Cache;
import net.filebot.ResourceManager;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class TVRageClient extends AbstractEpisodeListProvider {

	private String host = "services.tvrage.com";

	private String apikey;

	public TVRageClient(String apikey) {
		this.apikey = apikey;
	}

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
		Document dom = request("/feeds/full_search.php", singletonMap("show", query));

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
		Document dom = request("/feeds/full_show_info.php", singletonMap("sid", series.getSeriesId()));

		String seriesName = selectString("Show/name", dom);
		SimpleDate seriesStartDate = SimpleDate.parse(selectString("Show/started", dom), "MMM/dd/yyyy");
		Locale language = Locale.ENGLISH;

		List<Episode> episodes = new ArrayList<Episode>(25);
		List<Episode> specials = new ArrayList<Episode>(5);

		// episodes and specials
		for (Node node : selectNodes("//episode", dom)) {
			String title = getTextContent("title", node);
			Integer episodeNumber = getIntegerContent("seasonnum", node);
			String seasonIdentifier = getAttribute("no", node.getParentNode());
			Integer seasonNumber = seasonIdentifier == null ? null : new Integer(seasonIdentifier);
			SimpleDate airdate = SimpleDate.parse(getTextContent("airdate", node), "yyyy-MM-dd");

			SortOrder order = SortOrder.Airdate; // default order

			// check if we have season and episode number, if not it must be a special episode
			if (episodeNumber == null || seasonNumber == null) {
				// handle as special episode
				seasonNumber = getIntegerContent("season", node);
				int specialNumber = filterBySeason(specials, seasonNumber).size() + 1;
				specials.add(new Episode(seriesName, seriesStartDate, seasonNumber, null, title, null, specialNumber, order, language, airdate, searchResult));
			} else {
				// handle as normal episode
				if (sortOrder == SortOrder.Absolute) {
					episodeNumber = getIntegerContent("epnum", node);
					seasonNumber = null;
					order = SortOrder.Absolute;
				}

				episodes.add(new Episode(seriesName, seriesStartDate, seasonNumber, episodeNumber, title, null, null, order, language, airdate, searchResult));
			}
		}

		// add specials at the end
		episodes.addAll(specials);

		return episodes;
	}

	public Document request(String resource, Map<String, Object> parameters) throws IOException, SAXException {
		Map<String, Object> param = new LinkedHashMap<String, Object>(parameters);
		if (apikey != null) {
			param.put("key", apikey);
		}
		URL url = new URL("http", host, resource + "?" + encodeParameters(param, true));
		return getDocument(url);
	}

	@Override
	public URI getEpisodeListLink(SearchResult searchResult) {
		return URI.create(((TVRageSearchResult) searchResult).getLink() + "/episode_list/all");
	}

}
