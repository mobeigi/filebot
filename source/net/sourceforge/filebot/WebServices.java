
package net.sourceforge.filebot;


import static java.util.Arrays.*;
import static java.util.Collections.*;
import static net.sourceforge.filebot.Settings.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.filebot.audio.AcoustID;
import net.sourceforge.filebot.media.MediaDetection;
import net.sourceforge.filebot.web.AnidbClient;
import net.sourceforge.filebot.web.EpisodeListProvider;
import net.sourceforge.filebot.web.FanartTV;
import net.sourceforge.filebot.web.IMDbClient;
import net.sourceforge.filebot.web.LocalSearch;
import net.sourceforge.filebot.web.MovieIdentificationService;
import net.sourceforge.filebot.web.OpenSubtitlesClient;
import net.sourceforge.filebot.web.SearchResult;
import net.sourceforge.filebot.web.SerienjunkiesClient;
import net.sourceforge.filebot.web.SublightSubtitleClient;
import net.sourceforge.filebot.web.SubsceneSubtitleClient;
import net.sourceforge.filebot.web.SubtitleProvider;
import net.sourceforge.filebot.web.TMDbClient;
import net.sourceforge.filebot.web.TVRageClient;
import net.sourceforge.filebot.web.TheTVDBClient;
import net.sourceforge.filebot.web.VideoHashSubtitleService;


/**
 * Reuse the same web service client so login, cache, etc. can be shared.
 */
public final class WebServices {
	
	// episode dbs
	public static final TVRageClient TVRage = new TVRageClient();
	public static final AnidbClient AniDB = new AnidbClient(getApplicationName().toLowerCase(), 3);
	public static final SerienjunkiesClient Serienjunkies = new SerienjunkiesClient(getApplicationProperty("serienjunkies.apikey"));
	
	// extended TheTVDB module with local search
	public static final TheTVDBClient TheTVDB = new TheTVDBClientWithLocalSearch(getApplicationProperty("thetvdb.apikey"));
	
	// movie dbs
	public static final IMDbClient IMDb = new IMDbClient();
	public static final TMDbClient TMDb = new TMDbClient(getApplicationProperty("themoviedb.apikey"));
	
	// subtitle dbs
	public static final OpenSubtitlesClient OpenSubtitles = new OpenSubtitlesClient(String.format("%s %s", getApplicationName(), getApplicationVersion()));
	public static final SubsceneSubtitleClient Subscene = new SubsceneSubtitleClient();
	public static final SublightSubtitleClient Sublight = new SublightSubtitleClient();
	
	// misc
	public static final FanartTV FanartTV = new FanartTV(Settings.getApplicationProperty("fanart.tv.apikey"));
	public static final AcoustID AcoustID = new AcoustID(Settings.getApplicationProperty("acoustid.apikey"));
	
	
	public static EpisodeListProvider[] getEpisodeListProviders() {
		return new EpisodeListProvider[] { TVRage, AniDB, TheTVDB, Serienjunkies };
	}
	
	
	public static MovieIdentificationService[] getMovieIdentificationServices() {
		return new MovieIdentificationService[] { OpenSubtitles, IMDb, TMDb };
	}
	
	
	public static SubtitleProvider[] getSubtitleProviders() {
		return new SubtitleProvider[] { OpenSubtitles, Sublight, Subscene };
	}
	
	
	public static VideoHashSubtitleService[] getVideoHashSubtitleServices() {
		return new VideoHashSubtitleService[] { OpenSubtitles, Sublight };
	}
	
	
	public static EpisodeListProvider getEpisodeListProvider(String name) {
		for (EpisodeListProvider it : WebServices.getEpisodeListProviders()) {
			if (it.getName().equalsIgnoreCase(name))
				return it;
		}
		
		return null; // default
	}
	
	
	public static MovieIdentificationService getMovieIdentificationService(String name) {
		for (MovieIdentificationService it : getMovieIdentificationServices()) {
			if (it.getName().equalsIgnoreCase(name))
				return it;
		}
		
		return null; // default
	}
	
	
	private static class TheTVDBClientWithLocalSearch extends TheTVDBClient {
		
		public TheTVDBClientWithLocalSearch(String apikey) {
			super(apikey);
		}
		
		// index of local thetvdb data dump
		private static LocalSearch<SearchResult> localIndex;
		
		
		private synchronized LocalSearch<SearchResult> getLocalIndex() throws IOException {
			if (localIndex == null) {
				// fetch data dump
				TheTVDBSearchResult[] data = MediaDetection.releaseInfo.getTheTVDBIndex();
				
				// index data dump
				localIndex = new LocalSearch<SearchResult>(asList(data)) {
					
					@Override
					protected Set<String> getFields(SearchResult object) {
						return set(object.getName());
					}
				};
				
				// make local search more restrictive
				localIndex.setResultMinimumSimilarity(0.7f);
			}
			
			return localIndex;
		}
		
		
		@SuppressWarnings("unchecked")
		@Override
		public List<SearchResult> fetchSearchResult(final String query, final Locale locale) throws Exception {
			Callable<List<SearchResult>> apiSearch = new Callable<List<SearchResult>>() {
				
				@Override
				public List<SearchResult> call() throws Exception {
					return TheTVDBClientWithLocalSearch.super.fetchSearchResult(query, locale);
				}
			};
			Callable<List<SearchResult>> localSearch = new Callable<List<SearchResult>>() {
				
				@Override
				public List<SearchResult> call() throws Exception {
					try {
						return getLocalIndex().search(query);
					} catch (Exception e) {
						Logger.getLogger(TheTVDBClientWithLocalSearch.class.getName()).log(Level.SEVERE, e.getMessage(), e);
					}
					
					// let local search fail gracefully without affecting API search
					return emptyList();
				}
			};
			
			ExecutorService executor = Executors.newFixedThreadPool(2);
			try {
				Set<SearchResult> results = new LinkedHashSet<SearchResult>();
				
				for (Future<List<SearchResult>> resultSet : executor.invokeAll(asList(localSearch, apiSearch))) {
					try {
						results.addAll(resultSet.get());
					} catch (ExecutionException e) {
						if (e.getCause() instanceof Exception) {
							throw (Exception) e.getCause(); // unwrap cause
						}
					}
				}
				return new ArrayList<SearchResult>(results);
			} finally {
				executor.shutdownNow();
			}
		};
	}
	
	
	/**
	 * Dummy constructor to prevent instantiation.
	 */
	private WebServices() {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Initialize client settings from system properties
	 */
	static {
		String[] osdbLogin = getLogin("osdb.user");
		OpenSubtitles.setUser(osdbLogin[0], osdbLogin[1]);
		
		String[] sublightClientLogin = getLogin("sublight.client");
		Sublight.setClient(sublightClientLogin[0], sublightClientLogin[1]);
		
		String[] sublightUserLogin = getLogin("sublight.user");
		Sublight.setUser(sublightUserLogin[0], sublightUserLogin[1]);
	}
	
	
	private static String[] getLogin(String key) {
		// try system property first
		String login = System.getProperty(key);
		
		// try settings second
		if (login == null) {
			login = Settings.forPackage(WebServices.class).get(key, ":");
		}
		
		return login.split(":", 2);
	}
	
}
