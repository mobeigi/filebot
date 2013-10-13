package net.sourceforge.filebot.web;

import static net.sourceforge.filebot.web.EpisodeUtilities.*;
import static net.sourceforge.filebot.web.WebRequest.*;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;
import javax.swing.Icon;

import net.sourceforge.filebot.Cache;
import net.sourceforge.filebot.ResourceManager;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class SerienjunkiesClient extends AbstractEpisodeListProvider {

	private final String host = "api.serienjunkies.de";

	private final String apikey;

	public SerienjunkiesClient(String apikey) {
		this.apikey = apikey;
	}

	@Override
	public String getName() {
		return "Serienjunkies";
	}

	@Override
	public Icon getIcon() {
		return ResourceManager.getIcon("search.serienjunkies");
	}

	@Override
	public Locale getDefaultLocale() {
		return Locale.GERMAN;
	}

	@Override
	public ResultCache getCache() {
		return new ResultCache(host, Cache.getCache("web-datasource"));
	}

	@Override
	public List<SearchResult> search(String query, final Locale locale) throws Exception {
		// bypass automatic caching since search is based on locally cached data anyway
		return fetchSearchResult(query, locale);
	}

	@Override
	public List<SearchResult> fetchSearchResult(String query, Locale locale) throws Exception {
		LocalSearch<SearchResult> index = new LocalSearch<SearchResult>(getSeriesTitles()) {

			@Override
			protected Set<String> getFields(SearchResult series) {
				return set(series.getEffectiveNames());
			}
		};

		return new ArrayList<SearchResult>(index.search(query));
	}

	protected synchronized List<SerienjunkiesSearchResult> getSeriesTitles() throws IOException {
		ResultCache cache = getCache();

		@SuppressWarnings("unchecked")
		List<SerienjunkiesSearchResult> seriesList = (List) cache.getSearchResult(null, Locale.ROOT);
		if (seriesList != null) {
			return seriesList;
		}

		// fetch series data
		seriesList = new ArrayList<SerienjunkiesSearchResult>();

		JSONObject data = (JSONObject) request("/allseries.php?d=" + apikey);
		JSONArray list = (JSONArray) data.get("allseries");

		for (Object element : list) {
			JSONObject obj = (JSONObject) element;

			Integer sid = new Integer((String) obj.get("id"));
			String link = (String) obj.get("link");
			String mainTitle = (String) obj.get("short");
			String germanTitle = (String) obj.get("short_german");
			Date startDate = Date.parse((String) obj.get("firstepisode"), "yyyy-MM-dd");

			Set<String> titleSet = new LinkedHashSet<String>(2);
			for (String title : new String[] { germanTitle, mainTitle }) {
				if (title != null && title.length() > 0) {
					titleSet.add(title);
				}
			}
			if (titleSet.size() > 0) {
				List<String> titleList = new ArrayList<String>(titleSet);
				seriesList.add(new SerienjunkiesSearchResult(sid, link, titleList.get(0), titleList.subList(1, titleList.size()).toArray(new String[0]), startDate));
			}
		}

		// populate cache
		return cache.putSearchResult(null, Locale.ROOT, seriesList);
	}

	@Override
	public List<Episode> fetchEpisodeList(SearchResult searchResult, SortOrder sortOrder, Locale locale) throws IOException {
		SerienjunkiesSearchResult series = (SerienjunkiesSearchResult) searchResult;

		// fetch episode data
		List<Episode> episodes = new ArrayList<Episode>(25);

		String seriesName = series.getName();
		JSONObject data = (JSONObject) request("/allepisodes.php?d=" + apikey + "&q=" + series.getSeriesId());
		JSONArray list = (JSONArray) data.get("allepisodes");

		for (int i = 0; i < list.size(); i++) {
			JSONObject obj = (JSONObject) list.get(i);

			Integer season = new Integer((String) obj.get("season"));
			Integer episode = new Integer((String) obj.get("episode"));
			Date airdate = Date.parse((String) ((JSONObject) obj.get("airdates")).get("premiere"), "yyyy-MM-dd");

			String title = (String) obj.get("original");
			String german = (String) obj.get("german");
			if (title == null || (Locale.GERMAN.equals(locale) && german != null)) {
				title = german;
			}

			// enforce sanity
			if (title == null) {
				title = "";
			}

			episodes.add(new Episode(seriesName, series.getStartDate(), season, episode, title, i + 1, null, airdate, searchResult));
		}

		// make sure episodes are in ordered correctly
		sortEpisodes(episodes);

		return episodes;
	}

	protected Object request(String resource) throws IOException {
		URL url = new URL("https", host, resource);
		HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

		// disable SSL certificate validation
		connection.setSSLSocketFactory(createIgnoreCertificateSocketFactory());

		// fetch and parse JSON data
		Reader reader = getReader(connection);
		try {
			return JSONValue.parse(reader);
		} finally {
			reader.close();
		}
	}

	@Override
	public URI getEpisodeListLink(SearchResult searchResult) {
		return URI.create(String.format("http://www.serienjunkies.de/%s/alle-serien-staffeln.html", ((SerienjunkiesSearchResult) searchResult).getLink()));
	}

}
