package net.sourceforge.filebot;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static net.sourceforge.filebot.Settings.*;
import static net.sourceforge.filebot.media.MediaDetection.*;

import java.io.IOException;
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

import net.sourceforge.filebot.web.AcoustID;
import net.sourceforge.filebot.web.AnidbClient;
import net.sourceforge.filebot.web.AnidbSearchResult;
import net.sourceforge.filebot.web.AudioTrack;
import net.sourceforge.filebot.web.EpisodeListProvider;
import net.sourceforge.filebot.web.FanartTV;
import net.sourceforge.filebot.web.ID3Lookup;
import net.sourceforge.filebot.web.IMDbClient;
import net.sourceforge.filebot.web.LocalSearch;
import net.sourceforge.filebot.web.Movie;
import net.sourceforge.filebot.web.MovieIdentificationService;
import net.sourceforge.filebot.web.MusicIdentificationService;
import net.sourceforge.filebot.web.OpenSubtitlesClient;
import net.sourceforge.filebot.web.SearchResult;
import net.sourceforge.filebot.web.SerienjunkiesClient;
import net.sourceforge.filebot.web.SubtitleProvider;
import net.sourceforge.filebot.web.TMDbClient;
import net.sourceforge.filebot.web.TVRageClient;
import net.sourceforge.filebot.web.TVRageSearchResult;
import net.sourceforge.filebot.web.TheTVDBClient;
import net.sourceforge.filebot.web.TheTVDBSearchResult;
import net.sourceforge.filebot.web.VideoHashSubtitleService;

/**
 * Reuse the same web service client so login, cache, etc. can be shared.
 */
public final class WebServices {

	// episode dbs
	public static final TVRageClient TVRage = new TVRageClient();
	public static final AnidbClient AniDB = new AnidbClientWithLocalSearch(getApplicationName().toLowerCase(), 4);
	public static final SerienjunkiesClient Serienjunkies = new SerienjunkiesClient(getApplicationProperty("serienjunkies.apikey"));

	// extended TheTVDB module with local search
	public static final TheTVDBClientWithLocalSearch TheTVDB = new TheTVDBClientWithLocalSearch(getApplicationProperty("thetvdb.apikey"));

	// movie dbs
	public static final IMDbClient IMDb = new IMDbClient();
	public static final TMDbClient TMDb = new TMDbClient(getApplicationProperty("themoviedb.apikey"));

	// subtitle dbs
	public static final OpenSubtitlesClient OpenSubtitles = new OpenSubtitlesClient(String.format("%s %s", getApplicationName(), getApplicationVersion()));

	// misc
	public static final FanartTV FanartTV = new FanartTV(Settings.getApplicationProperty("fanart.tv.apikey"));
	public static final AcoustID AcoustID = new AcoustID(Settings.getApplicationProperty("acoustid.apikey"));

	public static EpisodeListProvider[] getEpisodeListProviders() {
		return new EpisodeListProvider[] { TheTVDB, AniDB, TVRage, Serienjunkies };
	}

	public static MovieIdentificationService[] getMovieIdentificationServices() {
		return new MovieIdentificationService[] { TMDb, IMDb, OpenSubtitles };
	}

	public static SubtitleProvider[] getSubtitleProviders() {
		return new SubtitleProvider[] { OpenSubtitles };
	}

	public static VideoHashSubtitleService[] getVideoHashSubtitleServices() {
		return new VideoHashSubtitleService[] { OpenSubtitles };
	}

	public static MusicIdentificationService[] getMusicIdentificationServices() {
		return new MusicIdentificationService[] { AcoustID, new ID3Lookup() };
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

	public static MusicIdentificationService getMusicIdentificationService(String name) {
		for (MusicIdentificationService it : getMusicIdentificationServices()) {
			if (it.getName().equalsIgnoreCase(name))
				return it;
		}

		return null; // default
	}

	public static Object getServiceBySearchResult(Object r) {
		if (r instanceof TheTVDBSearchResult)
			return WebServices.TheTVDB;
		if (r instanceof AnidbSearchResult)
			return WebServices.AniDB;
		if (r instanceof TVRageSearchResult)
			return WebServices.TVRage;
		if (r instanceof Movie)
			return WebServices.TMDb;
		if (r instanceof AudioTrack)
			return WebServices.AcoustID;

		return null;
	}

	public static class TheTVDBClientWithLocalSearch extends TheTVDBClient {

		public TheTVDBClientWithLocalSearch(String apikey) {
			super(apikey);
		}

		// index of local thetvdb data dump
		private static LocalSearch<SearchResult> localIndex;

		public synchronized LocalSearch<SearchResult> getLocalIndex() throws IOException {
			if (localIndex == null) {
				// fetch data dump
				TheTVDBSearchResult[] data = releaseInfo.getTheTVDBIndex();

				// index data dump
				localIndex = new LocalSearch<SearchResult>(asList(data)) {

					@Override
					protected Set<String> getFields(SearchResult object) {
						return set(object.getEffectiveNames());
					}
				};

				// make local search more restrictive
				localIndex.setResultMinimumSimilarity(0.7f);
			}

			return localIndex;
		}

		public SeriesInfo getSeriesInfoByLocalIndex(String name, Locale locale) throws Exception {
			List<SearchResult> results = getLocalIndex().search(name);
			if (results.size() > 0) {
				return getSeriesInfo((TheTVDBSearchResult) results.get(0), locale);
			}
			return null;
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
				return sortBySimilarity(results, singleton(query), getSeriesMatchMetric(), false);
			} finally {
				executor.shutdownNow();
			}
		};
	}

	public static class AnidbClientWithLocalSearch extends AnidbClient {

		public AnidbClientWithLocalSearch(String client, int clientver) {
			super(client, clientver);
		}

		@Override
		public List<AnidbSearchResult> getAnimeTitles() throws Exception {
			return asList(releaseInfo.getAnidbIndex());
		}
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
	}

	public static String[] getLogin(String key) {
		return Settings.forPackage(WebServices.class).get(key, ":").split(":", 2);
	}

	public static void setLogin(String id, String user, String password) {
		Settings settings = Settings.forPackage(WebServices.class);
		String value = user.length() > 0 && password.length() > 0 ? user + ":" + password : null;
		if (value == null) {
			user = "";
			password = "";
		}

		if (id.equals("osdb.user")) {
			settings.put(id, value);
			OpenSubtitles.setUser(user, password);
		} else {
			throw new IllegalArgumentException();
		}
	}

}
