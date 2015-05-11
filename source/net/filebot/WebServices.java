package net.filebot;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static net.filebot.Settings.*;
import static net.filebot.media.MediaDetection.*;
import static net.filebot.util.FileUtilities.*;

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

import net.filebot.media.XattrMetaInfoProvider;
import net.filebot.similarity.MetricAvg;
import net.filebot.web.AcoustIDClient;
import net.filebot.web.AnidbClient;
import net.filebot.web.AnidbSearchResult;
import net.filebot.web.EpisodeListProvider;
import net.filebot.web.FanartTVClient;
import net.filebot.web.ID3Lookup;
import net.filebot.web.LocalSearch;
import net.filebot.web.MovieIdentificationService;
import net.filebot.web.MusicIdentificationService;
import net.filebot.web.OMDbClient;
import net.filebot.web.OpenSubtitlesClient;
import net.filebot.web.OpenSubtitlesSearchResult;
import net.filebot.web.SearchResult;
import net.filebot.web.SubtitleProvider;
import net.filebot.web.TMDbClient;
import net.filebot.web.TVRageClient;
import net.filebot.web.TheTVDBClient;
import net.filebot.web.TheTVDBSearchResult;
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
	public static final OpenSubtitlesClient OpenSubtitles = new OpenSubtitlesClientWithLocalSearch(getApiKey("opensubtitles"), getApplicationVersion());

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

		public OpenSubtitlesClientWithLocalSearch(String name, String version) {
			super(name, version);
		}

		// index of local OpenSubtitles data dump
		private static LocalSearch<SearchResult> localIndex;

		public synchronized LocalSearch<SearchResult> getLocalIndex() throws IOException {
			if (localIndex == null) {
				// fetch data dump
				OpenSubtitlesSearchResult[] data = releaseInfo.getOpenSubtitlesIndex();

				// index data dump
				localIndex = new LocalSearch<SearchResult>(asList(data)) {

					@Override
					protected Set<String> getFields(SearchResult object) {
						return set(object.getEffectiveNames());
					}
				};
			}

			return localIndex;
		}

		@Override
		public synchronized List<SearchResult> search(final String query, final boolean byMovie, final boolean bySeries) throws Exception {
			List<SearchResult> results = getLocalIndex().search(query);

			return sortBySimilarity(results, singleton(query), new MetricAvg(getSeriesMatchMetric(), getMovieMatchMetric()), false);
		}

	}

	/**
	 * Dummy constructor to prevent instantiation.
	 */
	private WebServices() {
		throw new UnsupportedOperationException();
	}

	public static final String LOGIN_SEPARATOR = ":";
	public static final String LOGIN_OPENSUBTITLES = "osdb.user";

	/**
	 * Initialize client settings from system properties
	 */
	static {
		String[] osdbLogin = getLogin(LOGIN_OPENSUBTITLES);
		OpenSubtitles.setUser(osdbLogin[0], osdbLogin[1]);
	}

	public static String[] getLogin(String key) {
		try {
			String[] values = Settings.forPackage(WebServices.class).get(key, LOGIN_SEPARATOR).split(LOGIN_SEPARATOR, 2); // empty username/password by default
			if (values != null && values.length == 2 && values[0] != null && values[1] != null) {
				return values;
			}
		} catch (Exception e) {
			Logger.getLogger(WebServices.class.getName()).log(Level.WARNING, e.getMessage(), e);
		}
		return new String[] { "", "" };
	}

	public static void setLogin(String id, String user, String password) {
		// delete login
		if ((user == null || user.isEmpty()) && (password == null || password.isEmpty())) {
			if (LOGIN_OPENSUBTITLES.equals(id)) {
				OpenSubtitles.setUser("", "");
				Settings.forPackage(WebServices.class).remove(id);
			} else {
				throw new IllegalArgumentException();
			}
		} else {
			// enter login
			if (user == null || password == null || user.contains(LOGIN_SEPARATOR) || (user.isEmpty() && !password.isEmpty()) || (!user.isEmpty() && password.isEmpty())) {
				throw new IllegalArgumentException(String.format("Illegal login: %s:%s", user, password));
			}

			if (LOGIN_OPENSUBTITLES.equals(id)) {
				String password_md5 = md5(password);
				OpenSubtitles.setUser(user, password_md5);
				Settings.forPackage(WebServices.class).put(id, String.join(LOGIN_SEPARATOR, user, password_md5));
			} else {
				throw new IllegalArgumentException();
			}
		}
	}

}
