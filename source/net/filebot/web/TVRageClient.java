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
	public boolean hasSeasonSupport() {
		return true;
	}

	@Override
	protected SortOrder vetoRequestParameter(SortOrder order) {
		return SortOrder.Airdate;
	}

	@Override
	protected Locale vetoRequestParameter(Locale language) {
		return Locale.ENGLISH;
	}

	@Override
	public ResultCache getCache() {
		return new ResultCache(getName(), Cache.getCache("web-datasource"));
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
	protected SeriesData fetchSeriesData(SearchResult searchResult, SortOrder sortOrder, Locale locale) throws Exception {
		TVRageSearchResult series = (TVRageSearchResult) searchResult;
		Document dom = request("/feeds/full_show_info.php", singletonMap("sid", series.getId()));

		// parse series data
		Node seriesNode = selectNode("Show", dom);
		SeriesInfo seriesInfo = new SeriesInfo(getName(), sortOrder, locale, series.getId());
		seriesInfo.setAliasNames(searchResult.getEffectiveNames());

		seriesInfo.setName(getTextContent("name", seriesNode));
		seriesInfo.setNetwork(getTextContent("network", seriesNode));
		seriesInfo.setRuntime(getInteger(getTextContent("runtime", seriesNode)));
		seriesInfo.setStatus(getTextContent("status", seriesNode));

		seriesInfo.setGenres(getListContent("genre", null, getChild("genres", seriesNode)));
		seriesInfo.setStartDate(SimpleDate.parse(selectString("started", seriesNode), "MMM/dd/yyyy"));

		// parse episode data
		List<Episode> episodes = new ArrayList<Episode>(25);
		List<Episode> specials = new ArrayList<Episode>(5);

		// episodes and specials
		for (Node node : selectNodes("//episode", dom)) {
			String title = getTextContent("title", node);
			Integer episodeNumber = getInteger(getTextContent("seasonnum", node));
			String seasonIdentifier = getAttribute("no", node.getParentNode());
			Integer seasonNumber = seasonIdentifier == null ? null : new Integer(seasonIdentifier);
			SimpleDate airdate = SimpleDate.parse(getTextContent("airdate", node), "yyyy-MM-dd");

			// check if we have season and episode number, if not it must be a special episode
			if (episodeNumber == null || seasonNumber == null) {
				// handle as special episode
				seasonNumber = getInteger(getTextContent("season", node));
				int specialNumber = filterBySeason(specials, seasonNumber).size() + 1;
				specials.add(new Episode(seriesInfo.getName(), seasonNumber, null, title, null, specialNumber, airdate, new SeriesInfo(seriesInfo)));
			} else {
				// handle as normal episode
				if (sortOrder == SortOrder.Absolute) {
					episodeNumber = getInteger(getTextContent("epnum", node));
					seasonNumber = null;
				}
				episodes.add(new Episode(seriesInfo.getName(), seasonNumber, episodeNumber, title, null, null, airdate, new SeriesInfo(seriesInfo)));
			}
		}

		// add specials at the end
		episodes.addAll(specials);

		return new SeriesData(seriesInfo, episodes);
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
	protected SearchResult createSearchResult(int id) {
		return new TVRageSearchResult(null, id, null);
	}

	@Override
	public URI getEpisodeListLink(SearchResult searchResult) {
		return URI.create(((TVRageSearchResult) searchResult).getLink() + "/episode_list/all");
	}

}
