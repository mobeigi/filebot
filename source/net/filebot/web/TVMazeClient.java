package net.filebot.web;

import static java.util.Arrays.*;
import static java.util.stream.Collectors.*;
import static net.filebot.util.JsonUtilities.*;
import static net.filebot.web.WebRequest.*;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import javax.swing.Icon;

import net.filebot.Cache;
import net.filebot.ResourceManager;

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
		Object response = request("search/shows?q=" + encode(query, true));

		// TODO: FUTURE WORK: consider adding TVmaze aka titles for each result, e.g. http://api.tvmaze.com/shows/1/akas
		return stream(asMapArray(response)).map(it -> {
			Object show = it.get("show");
			Integer id = getInteger(show, "id");
			String name = getString(show, "name");

			return new TVMazeSearchResult(id, name);
		}).collect(toList());
	}

	protected SeriesInfo fetchSeriesInfo(TVMazeSearchResult show, SortOrder sortOrder, Locale locale) throws IOException {
		// e.g. http://api.tvmaze.com/shows/1
		Object response = request("shows/" + show.getId());

		String status = getStringValue(response, "status", String::new);
		SimpleDate premiered = getStringValue(response, "premiered", SimpleDate::parse);
		Integer runtime = getStringValue(response, "runtime", Integer::new);
		Object[] genres = getArray(response, "genres");
		Double rating = getStringValue(getMap(response, "rating"), "average", Double::new);

		SeriesInfo seriesInfo = new SeriesInfo(getName(), sortOrder, locale, show.getId());
		seriesInfo.setName(show.getName());
		seriesInfo.setAliasNames(show.getEffectiveNames());
		seriesInfo.setStatus(status);
		seriesInfo.setRuntime(runtime);
		seriesInfo.setStartDate(premiered);
		seriesInfo.setRating(rating);
		seriesInfo.setGenres(stream(genres).map(Objects::toString).collect(toList()));

		return seriesInfo;
	}

	@Override
	protected SeriesData fetchSeriesData(SearchResult searchResult, SortOrder sortOrder, Locale locale) throws Exception {
		TVMazeSearchResult show = (TVMazeSearchResult) searchResult;

		SeriesInfo seriesInfo = fetchSeriesInfo(show, sortOrder, locale);
		List<Episode> episodes = new ArrayList<Episode>(25);

		// e.g. http://api.tvmaze.com/shows/1/episodes
		Object response = request("shows/" + show.getId() + "/episodes");

		for (Map<?, ?> episode : asMapArray(response)) {
			String episodeTitle = getString(episode, "name");
			Integer seasonNumber = getInteger(episode, "season");
			Integer episodeNumber = getInteger(episode, "number");
			SimpleDate airdate = getStringValue(episode, "airdate", SimpleDate::parse);

			if (episodeNumber != null && episodeTitle != null) {
				episodes.add(new Episode(seriesInfo.getName(), seasonNumber, episodeNumber, episodeTitle, null, null, airdate, new SeriesInfo(seriesInfo)));
			}
		}

		return new SeriesData(seriesInfo, episodes);
	}

	public Object request(String resource) throws IOException {
		return new CachedJsonResource("http://api.tvmaze.com/" + resource).getJsonObject();
	}

	@Override
	public URI getEpisodeListLink(SearchResult searchResult) {
		return URI.create("http://www.tvmaze.com/shows/" + ((TVMazeSearchResult) searchResult).getId());
	}

}
