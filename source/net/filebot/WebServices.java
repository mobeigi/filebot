package net.filebot;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static java.util.stream.Collectors.*;
import static net.filebot.Logging.*;
import static net.filebot.Settings.*;
import static net.filebot.media.MediaDetection.*;
import static net.filebot.util.FileUtilities.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.stream.Stream;

import net.filebot.media.XattrMetaInfoProvider;
import net.filebot.similarity.MetricAvg;
import net.filebot.util.SystemProperty;
import net.filebot.web.AcoustIDClient;
import net.filebot.web.AnidbClient;
import net.filebot.web.Datasource;
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
import net.filebot.web.ShooterSubtitles;
import net.filebot.web.SubtitleProvider;
import net.filebot.web.SubtitleSearchResult;
import net.filebot.web.TMDbClient;
import net.filebot.web.TMDbTVClient;
import net.filebot.web.TVMazeClient;
import net.filebot.web.TheTVDBClient;
import net.filebot.web.VideoHashSubtitleService;

/**
 * Reuse the same web service client so login, cache, etc. can be shared.
 */
public final class WebServices {

	// movie sources
	public static final OMDbClient OMDb = new OMDbClient(getApiKey("omdb"));
	public static final TMDbClient TheMovieDB = new TMDbClientWithLocalSearch(getApiKey("themoviedb"), SystemProperty.of("net.filebot.WebServices.TheMovieDB.adult", Boolean::parseBoolean, false).get());

	// episode sources
	public static final TVMazeClient TVmaze = new TVMazeClient();
	public static final AnidbClient AniDB = new AnidbClientWithLocalSearch(getApiKey("anidb"), 6);

	// extended TheTVDB module with local search
	public static final TheTVDBClientWithLocalSearch TheTVDB = new TheTVDBClientWithLocalSearch(getApiKey("thetvdb"));
	public static final TMDbTVClient TheMovieDB_TV = new TMDbTVClient(TheMovieDB);

	// subtitle sources
	public static final OpenSubtitlesClient OpenSubtitles = new OpenSubtitlesClientWithLocalSearch(getApiKey("opensubtitles"), getApplicationVersion());
	public static final ShooterSubtitles Shooter = new ShooterSubtitles();

	// other sources
	public static final FanartTVClient FanartTV = new FanartTVClient(Settings.getApiKey("fanart.tv"));
	public static final AcoustIDClient AcoustID = new AcoustIDClient(Settings.getApiKey("acoustid"));
	public static final XattrMetaInfoProvider XattrMetaData = new XattrMetaInfoProvider();
	public static final ID3Lookup MediaInfoID3 = new ID3Lookup();

	public static Datasource[] getServices() {
		return new Datasource[] { TheMovieDB, OMDb, TheTVDB, AniDB, TheMovieDB_TV, TVmaze, AcoustID, MediaInfoID3, XattrMetaData, OpenSubtitles, Shooter, FanartTV };
	}

	public static MovieIdentificationService[] getMovieIdentificationServices() {
		return new MovieIdentificationService[] { TheMovieDB, OMDb };
	}

	public static EpisodeListProvider[] getEpisodeListProviders() {
		return new EpisodeListProvider[] { TheTVDB, AniDB, TheMovieDB_TV, TVmaze };
	}

	public static MusicIdentificationService[] getMusicIdentificationServices() {
		return new MusicIdentificationService[] { AcoustID, MediaInfoID3 };
	}

	public static SubtitleProvider[] getSubtitleProviders(Locale locale) {
		return new SubtitleProvider[] { OpenSubtitles };
	}

	public static VideoHashSubtitleService[] getVideoHashSubtitleServices(Locale locale) {
		switch (locale.getLanguage()) {
		case "zh":
			return new VideoHashSubtitleService[] { OpenSubtitles, Shooter }; // special support for 射手网 for Chinese language subtitles
		default:
			return new VideoHashSubtitleService[] { OpenSubtitles };
		}
	}

	public static Datasource getService(String name) {
		return getService(name, getServices());
	}

	public static EpisodeListProvider getEpisodeListProvider(String name) {
		return getService(name, getEpisodeListProviders());
	}

	public static MovieIdentificationService getMovieIdentificationService(String name) {
		return getService(name, getMovieIdentificationServices());
	}

	public static MusicIdentificationService getMusicIdentificationService(String name) {
		return getService(name, getMusicIdentificationServices());
	}

	public static <T extends Datasource> T getService(String name, T... services) {
		return stream(services).filter(it -> {
			return it.getIdentifier().equalsIgnoreCase(name);
		}).findFirst().orElse(null);
	}

	public static final ExecutorService requestThreadPool = Executors.newCachedThreadPool();

	public static class TMDbClientWithLocalSearch extends TMDbClient {

		public TMDbClientWithLocalSearch(String apikey, boolean adult) {
			super(apikey, adult);
		}

		// local TheMovieDB search index
		private Map<Integer, Resource<LocalSearch<Movie>>> localIndexPerYear = synchronizedMap(new HashMap<>());

		private Resource<LocalSearch<Movie>> getLocalIndex(int year) {
			return Resource.lazy(() -> {
				if (year > 0) {
					// limit search index to a given year (so we don't have to check all movies of all time all the time)
					Movie[] movies = stream(releaseInfo.getMovieList()).filter(m -> year == m.getYear()).toArray(Movie[]::new);

					// search by primary movie name and all known alias names
					return new LocalSearch<>(movies, Movie::getEffectiveNamesWithoutYear);
				}

				// check all movies of all time if release year is not known (but only compare to primary title for performance reasons)
				return new LocalSearch<>(releaseInfo.getMovieList(), m -> singleton(m.getName()));
			});
		}

		@Override
		public List<Movie> searchMovie(String movieName, int movieYear, Locale locale, boolean extendedInfo) throws Exception {
			// run local search and API search in parallel
			Future<List<Movie>> apiSearch = requestThreadPool.submit(() -> TMDbClientWithLocalSearch.super.searchMovie(movieName, movieYear, locale, extendedInfo));

			// the year might be off by 1 so we also check movies from the previous year and the next year
			Future<List<Movie>> localSearch1 = requestThreadPool.submit(() -> localIndexPerYear.computeIfAbsent(movieYear, this::getLocalIndex).get().search(movieName));
			Future<List<Movie>> localSearch2 = requestThreadPool.submit(() -> localIndexPerYear.computeIfAbsent(movieYear - 1, this::getLocalIndex).get().search(movieName));
			Future<List<Movie>> localSearch3 = requestThreadPool.submit(() -> localIndexPerYear.computeIfAbsent(movieYear + 1, this::getLocalIndex).get().search(movieName));

			// combine alias names into a single search results, and keep API search name as primary name
			return Stream.of(apiSearch.get(), localSearch1.get(), localSearch2.get(), localSearch3.get()).flatMap(List::stream).distinct().collect(toList());
		}
	}

	public static class TheTVDBClientWithLocalSearch extends TheTVDBClient {

		public TheTVDBClientWithLocalSearch(String apikey) {
			super(apikey);
		}

		// local TheTVDB search index
		private final Resource<LocalSearch<SearchResult>> localIndex = Resource.lazy(() -> new LocalSearch<SearchResult>(releaseInfo.getTheTVDBIndex(), SearchResult::getEffectiveNames));

		private SearchResult merge(SearchResult prime, List<SearchResult> group) {
			int id = prime.getId();
			String name = prime.getName();

			String[] aliasNames = group.stream().flatMap(it -> stream(it.getAliasNames())).filter(n -> !n.equals(name)).distinct().toArray(String[]::new);
			return new SearchResult(id, name, aliasNames);
		}

		@Override
		public List<SearchResult> fetchSearchResult(String query, Locale locale) throws Exception {
			// run local search and API search in parallel
			Future<List<SearchResult>> apiSearch = requestThreadPool.submit(() -> TheTVDBClientWithLocalSearch.super.fetchSearchResult(query, locale));
			Future<List<SearchResult>> localSearch = requestThreadPool.submit(() -> localIndex.get().search(query));

			// combine alias names into a single search results, and keep API search name as primary name
			Map<Integer, SearchResult> results = Stream.of(apiSearch.get(), localSearch.get()).flatMap(List::stream).collect(groupingBy(SearchResult::getId, LinkedHashMap::new, collectingAndThen(toList(), group -> merge(group.get(0), group))));

			return sortBySimilarity(results.values(), singleton(query), getSeriesMatchMetric());
		}
	}

	public static class AnidbClientWithLocalSearch extends AnidbClient {

		public AnidbClientWithLocalSearch(String client, int clientver) {
			super(client, clientver);
		}

		@Override
		public SearchResult[] getAnimeTitles() throws Exception {
			return releaseInfo.getAnidbIndex();
		}
	}

	public static class OpenSubtitlesClientWithLocalSearch extends OpenSubtitlesClient {

		public OpenSubtitlesClientWithLocalSearch(String name, String version) {
			super(name, version);
		}

		// local OpenSubtitles search index
		private final Resource<LocalSearch<SubtitleSearchResult>> localIndex = Resource.lazy(() -> new LocalSearch<SubtitleSearchResult>(releaseInfo.getOpenSubtitlesIndex(), SearchResult::getEffectiveNames));

		@Override
		public List<SubtitleSearchResult> search(final String query) throws Exception {
			return sortBySimilarity(localIndex.get().search(query), singleton(query), new MetricAvg(getSeriesMatchMetric(), getMovieMatchMetric()));
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
			debug.log(Level.SEVERE, e.getMessage(), e);
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
				Settings.forPackage(WebServices.class).put(id, user + LOGIN_SEPARATOR + password_md5);
			} else {
				throw new IllegalArgumentException();
			}
		}
	}

}
