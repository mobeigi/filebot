package net.filebot.web;

import static java.util.Arrays.*;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.filebot.Cache;
import net.filebot.Cache.Key;

public abstract class AbstractEpisodeListProvider implements EpisodeListProvider {

	protected abstract List<SearchResult> fetchSearchResult(String query, Locale locale) throws Exception;

	protected abstract SeriesData fetchSeriesData(SearchResult searchResult, SortOrder sortOrder, Locale locale) throws Exception;

	protected abstract SearchResult createSearchResult(int id);

	protected abstract ResultCache getCache();

	protected abstract SortOrder vetoRequestParameter(SortOrder order);

	protected abstract Locale vetoRequestParameter(Locale language);

	@Override
	public List<SearchResult> search(String query, Locale language) throws Exception {
		List<SearchResult> results = getCache().getSearchResult(query, language);
		if (results != null) {
			return results;
		}

		// perform actual search
		results = fetchSearchResult(query, language);

		// cache results and return
		return getCache().putSearchResult(query, language, results);
	}

	@Override
	public List<Episode> getEpisodeList(SearchResult searchResult, SortOrder sortOrder, Locale language) throws Exception {
		return getSeriesData(searchResult, sortOrder, language).getEpisodeList();
	}

	@Override
	public List<Episode> getEpisodeList(int id, SortOrder order, Locale language) throws Exception {
		return getEpisodeList(createSearchResult(id), order, language);
	}

	@Override
	public SeriesInfo getSeriesInfo(SearchResult searchResult, Locale language) throws Exception {
		return getSeriesData(searchResult, null, language).getSeriesInfo();
	}

	@Override
	public SeriesInfo getSeriesInfo(int id, Locale language) throws Exception {
		return getSeriesInfo(createSearchResult(id), language);
	}

	protected SeriesData getSeriesData(SearchResult searchResult, SortOrder order, Locale language) throws Exception {
		// override preferences if requested parameters are not supported
		order = vetoRequestParameter(order);
		language = vetoRequestParameter(language);

		SeriesData data = getCache().getSeriesData(searchResult, order, language);
		if (data != null) {
			return data;
		}

		// perform actual lookup
		data = fetchSeriesData(searchResult, order, language);

		// cache results and return
		return getCache().putSeriesData(searchResult, order, language, data);
	}

	protected static class SeriesData implements Serializable {

		public SeriesInfo seriesInfo;
		public Episode[] episodeList;

		public SeriesData(SeriesInfo seriesInfo, List<Episode> episodeList) {
			this.seriesInfo = seriesInfo;
			this.episodeList = episodeList.toArray(new Episode[episodeList.size()]);
		}

		public SeriesInfo getSeriesInfo() {
			return seriesInfo.clone();
		}

		public List<Episode> getEpisodeList() {
			return asList(episodeList.clone());
		}

	}

	protected static class ResultCache {

		private final String id;
		private final Cache cache;

		public ResultCache(String id, Cache cache) {
			this.id = id;
			this.cache = cache;
		}

		protected String normalize(String query) {
			return query == null ? null : query.trim().toLowerCase();
		}

		public <T extends SearchResult> List<T> putSearchResult(String query, Locale locale, List<T> value) {
			putData("SearchResult", normalize(query), locale, value.toArray(new SearchResult[value.size()]));
			return value;
		}

		public List<SearchResult> getSearchResult(String query, Locale locale) {
			SearchResult[] data = getData("SearchResult", normalize(query), locale, SearchResult[].class);
			return data == null ? null : asList(data);
		}

		public SeriesData putSeriesData(SearchResult key, SortOrder sortOrder, Locale locale, SeriesData seriesData) {
			putData("SeriesData." + sortOrder.name(), key, locale, seriesData);
			return seriesData;
		}

		public SeriesData getSeriesData(SearchResult key, SortOrder sortOrder, Locale locale) {
			return getData("SeriesData." + sortOrder.name(), key, locale, SeriesData.class);
		}

		public <T> T putData(Object category, Object key, Locale locale, T object) {
			try {
				cache.put(new Key(id, category, locale, key), object);
			} catch (Exception e) {
				Logger.getLogger(AbstractEpisodeListProvider.class.getName()).log(Level.WARNING, e.getMessage());
			}
			return object;
		}

		public <T> T getData(Object category, Object key, Locale locale, Class<T> type) {
			try {
				return cache.get(new Key(id, category, locale, key), type);
			} catch (Exception e) {
				Logger.getLogger(AbstractEpisodeListProvider.class.getName()).log(Level.WARNING, e.getMessage(), e);
			}
			return null;
		}

	}

}
