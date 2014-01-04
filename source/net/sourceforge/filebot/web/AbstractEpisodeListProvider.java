package net.sourceforge.filebot.web;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.filebot.Cache;
import net.sourceforge.filebot.Cache.Key;

public abstract class AbstractEpisodeListProvider implements EpisodeListProvider {

	@Override
	public boolean hasSingleSeasonSupport() {
		return true;
	}

	@Override
	public boolean hasLocaleSupport() {
		return false;
	}

	protected abstract List<SearchResult> fetchSearchResult(String query, Locale locale) throws Exception;

	protected abstract List<Episode> fetchEpisodeList(SearchResult searchResult, SortOrder sortOrder, Locale locale) throws Exception;

	public List<SearchResult> search(String query) throws Exception {
		return search(query, getDefaultLocale());
	}

	@Override
	public List<SearchResult> search(String query, Locale locale) throws Exception {
		ResultCache cache = getCache();
		List<SearchResult> results = (cache != null) ? cache.getSearchResult(query, locale) : null;
		if (results != null) {
			return results;
		}

		// perform actual search
		results = fetchSearchResult(query, locale);

		// cache results and return
		return (cache != null) ? cache.putSearchResult(query, locale, results) : results;
	}

	public List<Episode> getEpisodeList(SearchResult searchResult, String sortOrder, String locale) throws Exception {
		return getEpisodeList(searchResult, sortOrder == null ? SortOrder.Airdate : SortOrder.forName(sortOrder), new Locale(locale));
	}

	public List<Episode> getEpisodeList(SearchResult searchResult) throws Exception {
		return getEpisodeList(searchResult, SortOrder.Airdate, getDefaultLocale());
	}

	@Override
	public List<Episode> getEpisodeList(SearchResult searchResult, SortOrder sortOrder, Locale locale) throws Exception {
		ResultCache cache = getCache();
		List<Episode> episodes = (cache != null) ? cache.getEpisodeList(searchResult, sortOrder, locale) : null;
		if (episodes != null) {
			return episodes;
		}

		// perform actual search
		episodes = fetchEpisodeList(searchResult, sortOrder, locale);

		// cache results and return
		return (cache != null) ? cache.putEpisodeList(searchResult, sortOrder, locale, episodes) : episodes;
	}

	public Locale getDefaultLocale() {
		return Locale.ENGLISH;
	}

	public ResultCache getCache() {
		return null;
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
			try {
				cache.put(new Key(id, normalize(query), locale), value.toArray(new SearchResult[0]));
			} catch (Exception e) {
				Logger.getLogger(AbstractEpisodeListProvider.class.getName()).log(Level.WARNING, e.getMessage());
			}

			return value;
		}

		public List<SearchResult> getSearchResult(String query, Locale locale) {
			try {
				SearchResult[] results = cache.get(new Key(id, normalize(query), locale), SearchResult[].class);
				if (results != null) {
					return Arrays.asList(results);
				}
			} catch (Exception e) {
				Logger.getLogger(AbstractEpisodeListProvider.class.getName()).log(Level.WARNING, e.getMessage(), e);
			}

			return null;
		}

		public List<Episode> putEpisodeList(SearchResult key, SortOrder sortOrder, Locale locale, List<Episode> episodes) {
			try {
				cache.put(new Key(id, key, sortOrder, locale), episodes.toArray(new Episode[0]));
			} catch (Exception e) {
				Logger.getLogger(AbstractEpisodeListProvider.class.getName()).log(Level.WARNING, e.getMessage());
			}

			return episodes;
		}

		public List<Episode> getEpisodeList(SearchResult key, SortOrder sortOrder, Locale locale) {
			try {
				Episode[] episodes = cache.get(new Key(id, key, sortOrder, locale), Episode[].class);
				if (episodes != null) {
					return Arrays.asList(episodes);
				}
			} catch (Exception e) {
				Logger.getLogger(AbstractEpisodeListProvider.class.getName()).log(Level.WARNING, e.getMessage(), e);
			}

			return null;
		}

		public void putData(Object category, Object key, Locale locale, Object object) {
			try {
				cache.put(new Key(id, category, locale, key), object);
			} catch (Exception e) {
				Logger.getLogger(AbstractEpisodeListProvider.class.getName()).log(Level.WARNING, e.getMessage());
			}
		}

		public <T> T getData(Object category, Object key, Locale locale, Class<T> type) {
			try {
				T value = cache.get(new Key(id, category, locale, key), type);
				if (value != null) {
					return value;
				}
			} catch (Exception e) {
				Logger.getLogger(AbstractEpisodeListProvider.class.getName()).log(Level.WARNING, e.getMessage(), e);
			}

			return null;
		}

	}

}
