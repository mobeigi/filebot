
package net.sourceforge.filebot.web;


import static net.sourceforge.filebot.web.EpisodeUtilities.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;


public abstract class AbstractEpisodeListProvider implements EpisodeListProvider {
	
	@Override
	public boolean hasSingleSeasonSupport() {
		return true;
	}
	

	@Override
	public boolean hasLocaleSupport() {
		return false;
	}
	

	public List<SearchResult> search(String query) throws Exception {
		return search(query, getDefaultLocale());
	}
	

	public List<Episode> getEpisodeList(SearchResult searchResult) throws Exception {
		return getEpisodeList(searchResult, getDefaultLocale());
	}
	

	public List<Episode> getEpisodeList(SearchResult searchResult, int season) throws Exception {
		return getEpisodeList(searchResult, season, getDefaultLocale());
	}
	

	public Locale getDefaultLocale() {
		return Locale.ENGLISH;
	}
	

	@Override
	public List<Episode> getEpisodeList(SearchResult searchResult, int season, Locale locale) throws Exception {
		List<Episode> all = getEpisodeList(searchResult, locale);
		List<Episode> eps = filterBySeason(all, season);
		
		if (eps.isEmpty()) {
			throw new SeasonOutOfBoundsException(searchResult.getName(), season, getLastSeason(all));
		}
		
		return eps;
	}
	

	protected static class ResultCache {
		
		private final String id;
		private final Cache cache;
		

		public ResultCache(String id, Cache cache) {
			this.id = id;
			this.cache = cache;
		}
		

		public void putSearchResult(String key, Collection<? extends SearchResult> value) {
			cache.put(new Element(key(id, "SearchResult", key), value.toArray(new SearchResult[0])));
		}
		

		public List<SearchResult> getSearchResult(String key) {
			try {
				Element element = cache.get(key(id, "SearchResult", key));
				if (element != null) {
					return Arrays.asList(((SearchResult[]) element.getValue()));
				}
			} catch (Exception e) {
				Logger.getLogger(AbstractEpisodeListProvider.class.getName()).log(Level.WARNING, e.getMessage(), e);
			}
			
			return null;
		}
		

		public void putEpisodeList(int key, Locale language, List<Episode> episodes) {
			cache.put(new Element(key(id, "EpisodeList", key, language.getLanguage()), episodes.toArray(new Episode[0])));
		}
		

		public List<Episode> getEpisodeList(int key, Locale language) {
			try {
				Element element = cache.get(key(id, "EpisodeList", key, language.getLanguage()));
				if (element != null) {
					return Arrays.asList((Episode[]) element.getValue());
				}
			} catch (Exception e) {
				Logger.getLogger(AbstractEpisodeListProvider.class.getName()).log(Level.WARNING, e.getMessage(), e);
			}
			
			return null;
		}
		

		private String key(Object... key) {
			return Arrays.toString(key);
		}
	}
	
}
