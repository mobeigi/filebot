package net.filebot;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static net.filebot.Settings.*;
import static net.filebot.media.MediaDetection.*;

import java.io.IOException;
import java.net.URI;
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

import net.filebot.media.XattrMetaInfoProvider;
import net.filebot.similarity.MetricAvg;
import net.filebot.web.AcoustIDClient;
import net.filebot.web.AnidbClient;
import net.filebot.web.AnidbSearchResult;
import net.filebot.web.EpisodeListProvider;
import net.filebot.web.FanartTVClient;
import net.filebot.web.ID3Lookup;
import net.filebot.web.LocalSearch;
import net.filebot.web.Movie;
import net.filebot.web.MovieIdentificationService;
import net.filebot.web.MusicIdentificationService;
import net.filebot.web.OMDbClient;
import net.filebot.web.OpenSubtitlesClient;
import net.filebot.web.SearchResult;
import net.filebot.web.SubtitleDescriptor;
import net.filebot.web.SubtitleProvider;
import net.filebot.web.TMDbClient;
import net.filebot.web.TVRageClient;
import net.filebot.web.TheTVDBClient;
import net.filebot.web.TheTVDBSearchResult;
import net.filebot.web.TheTVDBSeriesInfo;
import net.filebot.web.VideoHashSubtitleService;

/**
 * Reuse the same web service client so login, cache, etc. can be shared.
 */
public final class WebServices {

	// episode dbs
	public static final TVRageClient TVRage = new TVRageClient(getApiKey("tvrage"));
	public static final AnidbClient AniDB = new AnidbClientWithLocalSearch(getApiKey("anidb"), 6);

	// extended TheTVDB module with local search
	public static final TheTVDBClientWithLocalSearch TheTVDB = new TheTVDBClientWithLocalSearch(getApiKey("thetvdb"));

	// movie dbs
	public static final OMDbClient OMDb = new OMDbClient();
	public static final TMDbClient TheMovieDB = new TMDbClient(getApiKey("themoviedb"));

	// subtitle dbs
	public static final OpenSubtitlesClient OpenSubtitles = new OpenSubtitlesClientWithLocalSearch(getApiKey("opensubtitles"), getApplicationVersion(), TheTVDB, TheMovieDB);

	// misc
	public static final FanartTVClient FanartTV = new FanartTVClient(Settings.getApiKey("fanart.tv"));
	public static final AcoustIDClient AcoustID = new AcoustIDClient(Settings.getApiKey("acoustid"));
	public static final XattrMetaInfoProvider XattrMetaData = new XattrMetaInfoProvider();

	public static EpisodeListProvider[] getEpisodeListProviders() {
		return new EpisodeListProvider[] { TheTVDB, AniDB, TVRage };
	}

	public static MovieIdentificationService[] getMovieIdentificationServices() {
		return new MovieIdentificationService[] { TheMovieDB, OMDb };
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

	public static final ExecutorService requestThreadPool = Executors.newCachedThreadPool();

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

		@Override
		public List<SearchResult> fetchSearchResult(final String query, final Locale locale) throws Exception {
			Callable<List<SearchResult>> apiSearch = () -> TheTVDBClientWithLocalSearch.super.fetchSearchResult(query, locale);
			Callable<List<SearchResult>> localSearch = () -> getLocalIndex().search(query);

			Set<SearchResult> results = new LinkedHashSet<SearchResult>();
			for (Future<List<SearchResult>> resultSet : requestThreadPool.invokeAll(asList(localSearch, apiSearch))) {
				try {
					results.addAll(resultSet.get());
				} catch (ExecutionException e) {
					if (e.getCause() instanceof Exception) {
						throw (Exception) e.getCause(); // unwrap cause
					}
				}
			}
			return sortBySimilarity(results, singleton(query), getSeriesMatchMetric(), false);
		}
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

	public static class OpenSubtitlesClientWithLocalSearch extends OpenSubtitlesClient {

		private final EpisodeListProvider seriesIndex;
		private final MovieIdentificationService movieIndex;

		public OpenSubtitlesClientWithLocalSearch(String name, String version, EpisodeListProvider seriesIndex, MovieIdentificationService movieIndex) {
			super(name, version);
			this.seriesIndex = seriesIndex;
			this.movieIndex = movieIndex;
		}

		@Override
		public synchronized List<SearchResult> search(final String query, final boolean byMovie, final boolean bySeries) throws Exception {
			List<Callable<List<? extends SearchResult>>> queries = new ArrayList<>(2);
			if (byMovie) {
				queries.add(() -> movieIndex.searchMovie(query, Locale.ENGLISH));
			}
			if (bySeries) {
				queries.add(() -> seriesIndex.search(query, Locale.ENGLISH));
			}

			Set<SearchResult> results = new LinkedHashSet<SearchResult>();
			for (Future<List<? extends SearchResult>> resultSet : requestThreadPool.invokeAll(queries)) {
				try {
					results.addAll(resultSet.get());
				} catch (ExecutionException e) {
					if (e.getCause() instanceof Exception) {
						throw (Exception) e.getCause(); // unwrap cause
					}
				}
			}
			return sortBySimilarity(results, singleton(query), new MetricAvg(getSeriesMatchMetric(), getMovieMatchMetric()), false);
		}

		@Override
		public synchronized List<SubtitleDescriptor> getSubtitleList(SearchResult searchResult, String languageName) throws Exception {
			Movie id = getIMDbID(searchResult);
			if (id != null) {
				return super.getSubtitleList(getIMDbID(searchResult), languageName);
			}
			return emptyList();
		}

		@Override
		public URI getSubtitleListLink(SearchResult searchResult, String languageName) {
			try {
				Movie id = getIMDbID(searchResult);
				if (id != null) {
					return super.getSubtitleListLink(id, languageName);
				}
			} catch (Exception e) {
				Logger.getLogger(WebServices.class.getName()).log(Level.WARNING, e.getMessage());
			}
			return null;
		}

		public Movie getIMDbID(SearchResult result) throws Exception {
			if (result instanceof TheTVDBSearchResult) {
				TheTVDBSearchResult searchResult = (TheTVDBSearchResult) result;
				TheTVDBSeriesInfo seriesInfo = (TheTVDBSeriesInfo) ((TheTVDBClient) seriesIndex).getSeriesInfo(searchResult, Locale.ENGLISH);
				if (seriesInfo.getImdbId() != null) {
					int imdbId = grepImdbId(seriesInfo.getImdbId()).iterator().next();
					return new Movie(seriesInfo.getName(), seriesInfo.getStartDate().getYear(), imdbId, -1);
				}
			}
			if (result instanceof Movie) {
				Movie m = (Movie) result;
				if (m.getImdbId() > 0)
					return m;

				// fetch extended movie info
				m = movieIndex.getMovieDescriptor(m, Locale.ENGLISH);
				if (m.getImdbId() > 0)
					return m;
			}
			return null;
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
		try {
			String[] values = Settings.forPackage(WebServices.class).get(key, ":").split(":", 2); // empty username/password by default
			if (values != null && values.length == 2 && values[0] != null && values[1] != null) {
				return values;
			}
		} catch (Exception e) {
			Logger.getLogger(WebServices.class.getName()).log(Level.WARNING, e.getMessage(), e);
		}
		return new String[] { "", "" };
	}

	public static void setLogin(String id, String user, String password) {
		if (user == null || password == null || user.contains(":") || (user.isEmpty() && !password.isEmpty()) || (!user.isEmpty() && password.isEmpty())) {
			throw new IllegalArgumentException(String.format("Illegal login: %s:%s", user, password));
		}

		Settings settings = Settings.forPackage(WebServices.class);
		String value = user.isEmpty() && password.isEmpty() ? null : user + ":" + password;

		if (id.equals("osdb.user")) {
			settings.put(id, value);
			OpenSubtitles.setUser(user, password);
		} else {
			throw new IllegalArgumentException();
		}
	}

}
