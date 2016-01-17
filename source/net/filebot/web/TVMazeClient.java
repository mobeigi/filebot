package net.filebot.web;

import static net.filebot.web.WebRequest.*;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.swing.Icon;

import net.filebot.Cache;
import net.filebot.ResourceManager;

import com.cedarsoftware.util.io.JsonObject;

public class TVMazeClient extends AbstractEpisodeListProvider {

	@Override
	public String getName() {
		return "TVmaze";
	}

	@Override
	public Icon getIcon() {
		return ResourceManager.getIcon("search.tvmaze");
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
	protected SearchResult createSearchResult(int id) {
		return new TVMazeSearchResult(id, null);
	}

	@Override
	public ResultCache getCache() {
		return new ResultCache(getName(), Cache.getCache("web-datasource"));
	}

	@Override
	public List<SearchResult> fetchSearchResult(String query, Locale locale) throws IOException {
		// e.g. http://api.tvmaze.com/search/shows?q=girls
		JsonObject<?, ?> response = request("search/shows?q=" + encode(query, true));

		List<SearchResult> results = new ArrayList<SearchResult>();
		if (response.isArray()) {
			for (Object result : response.getArray()) {
				Map<?, ?> show = (Map<?, ?>) ((Map<?, ?>) result).get("show");

				Integer id = getValue(show, "id", Integer::new);
				String name = getValue(show, "name", String::new);

				// FUTURE WORK: consider adding TVmaze aka titles for each result, e.g. http://api.tvmaze.com/shows/1/akas
				results.add(new TVMazeSearchResult(id, name));
			}
		}

		return results;
	}

	protected SeriesInfo fetchSeriesInfo(TVMazeSearchResult show, SortOrder sortOrder, Locale locale) throws IOException {
		// e.g. http://api.tvmaze.com/shows/1
		JsonObject<?, ?> response = request("shows/" + show.getId());

		String status = getValue(response, "status", String::new);
		SimpleDate premiered = getValue(response, "premiered", s -> SimpleDate.parse(s, "yyyy-MM-dd"));
		Integer runtime = getValue(response, "runtime", Integer::new);
		JsonObject<?, ?> genres = (JsonObject<?, ?>) response.get("genres");
		JsonObject<?, ?> rating = (JsonObject<?, ?>) response.get("rating");

		SeriesInfo seriesInfo = new SeriesInfo(getName(), sortOrder, locale, show.getId());
		seriesInfo.setName(show.getName());
		seriesInfo.setAliasNames(show.getEffectiveNames());
		seriesInfo.setStatus(status);
		seriesInfo.setRuntime(runtime);
		seriesInfo.setStartDate(premiered);

		if (genres != null && genres.isArray()) {
			seriesInfo.setGenres(Arrays.stream(genres.getArray()).map(Objects::toString).collect(Collectors.toList()));
		}
		if (rating != null && !rating.isEmpty()) {
			seriesInfo.setRating(getValue(rating, "average", Double::new));
		}

		return seriesInfo;
	}

	@Override
	protected SeriesData fetchSeriesData(SearchResult searchResult, SortOrder sortOrder, Locale locale) throws Exception {
		TVMazeSearchResult show = (TVMazeSearchResult) searchResult;

		SeriesInfo seriesInfo = fetchSeriesInfo(show, sortOrder, locale);

		// e.g. http://api.tvmaze.com/shows/1/episodes
		JsonObject<?, ?> response = request("shows/" + show.getId() + "/episodes");

		List<Episode> episodes = new ArrayList<Episode>(25);

		if (response.isArray()) {
			for (Object element : response.getArray()) {
				JsonObject<?, ?> episode = (JsonObject<?, ?>) element;

				String episodeTitle = getValue(episode, "name", String::new);
				Integer seasonNumber = getValue(episode, "season", Integer::new);
				Integer episodeNumber = getValue(episode, "number", Integer::new);
				SimpleDate airdate = getValue(episode, "airdate", s -> SimpleDate.parse(s, "yyyy-MM-dd"));

				episodes.add(new Episode(seriesInfo.getName(), seasonNumber, episodeNumber, episodeTitle, null, null, airdate, new SeriesInfo(seriesInfo)));
			}
		}

		return new SeriesData(seriesInfo, episodes);
	}

	private <V> V getValue(Map<?, ?> json, String key, Function<String, V> converter) {
		try {
			Object value = json.get(key);
			if (value != null) {
				return converter.apply(value.toString());
			}
		} catch (Exception e) {
			Logger.getLogger(TVMazeClient.class.getName()).log(Level.WARNING, "Illegal " + key + " value: " + json);
		}
		return null;
	}

	public JsonObject<?, ?> request(String resource) throws IOException {
		return new CachedJsonResource("http://api.tvmaze.com/" + resource).getJSON();
	}

	@Override
	public URI getEpisodeListLink(SearchResult searchResult) {
		return URI.create("http://www.tvmaze.com/shows/" + ((TVMazeSearchResult) searchResult).getId());
	}

}
