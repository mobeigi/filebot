package net.filebot;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static java.util.stream.Collectors.*;
import static net.filebot.Logging.*;
import static net.filebot.Settings.*;
import static net.filebot.media.MediaDetection.*;
import static net.filebot.util.FileUtilities.*;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;

import net.filebot.media.XattrMetaInfoProvider;
import net.filebot.similarity.MetricAvg;
import net.filebot.web.AcoustIDClient;
import net.filebot.web.AnidbClient;
import net.filebot.web.Datasource;
import net.filebot.web.EpisodeListProvider;
import net.filebot.web.FanartTVClient;
import net.filebot.web.ID3Lookup;
import net.filebot.web.LocalSearch;
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
import one.util.streamex.StreamEx;

/**
 * Reuse the same web service client so login, cache, etc. can be shared.
 */
public final class WebServices {

	// movie sources
	public static final OMDbClient OMDb = new OMDbClient();
	public static final TMDbClient TheMovieDB = new TMDbClient(getApiKey("themoviedb"));

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

	public static EpisodeListProvider[] getEpisodeListProviders() {
		return new EpisodeListProvider[] { TheTVDB, AniDB, TheMovieDB_TV, TVmaze };
	}

	public static MovieIdentificationService[] getMovieIdentificationServices() {
		return new MovieIdentificationService[] { TheMovieDB, OMDb };
	}

	public static SubtitleProvider[] getSubtitleProviders() {
		return new SubtitleProvider[] { OpenSubtitles };
	}

	public static VideoHashSubtitleService[] getVideoHashSubtitleServices(Locale locale) {
		if (locale.equals(Locale.CHINESE))
			return new VideoHashSubtitleService[] { OpenSubtitles, Shooter };
		else
			return new VideoHashSubtitleService[] { OpenSubtitles };
	}

	public static MusicIdentificationService[] getMusicIdentificationServices() {
		return new MusicIdentificationService[] { AcoustID, MediaInfoID3 };
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

	private static <T extends Datasource> T getService(String name, T[] services) {
		return StreamEx.of(services).findFirst(it -> it.getIdentifier().equalsIgnoreCase(name) || it.getName().equalsIgnoreCase(name)).orElse(null);
	}

	public static final ExecutorService requestThreadPool = Executors.newCachedThreadPool();

	public static class TheTVDBClientWithLocalSearch extends TheTVDBClient {

		public TheTVDBClientWithLocalSearch(String apikey) {
			super(apikey);
		}

		// local TheTVDB search index
		private final Resource<LocalSearch<SearchResult>> localIndex = Resource.lazy(() -> {
			return new LocalSearch<SearchResult>(releaseInfo.getTheTVDBIndex(), SearchResult::getEffectiveNames);
		}).memoize();

		private SearchResult merge(SearchResult prime, List<SearchResult> group) {
			int id = prime.getId();
			String name = prime.getName();
			String[] aliasNames = StreamEx.of(group).flatMap(it -> stream(it.getAliasNames())).remove(name::equals).distinct().toArray(String[]::new);
			return new SearchResult(id, name, aliasNames);
		}

		@Override
		public List<SearchResult> fetchSearchResult(final String query, final Locale locale) throws Exception {
			// run local search and API search in parallel
			Future<List<SearchResult>> apiSearch = requestThreadPool.submit(() -> TheTVDBClientWithLocalSearch.super.fetchSearchResult(query, locale));
			Future<List<SearchResult>> localSearch = requestThreadPool.submit(() -> localIndex.get().search(query));

			// combine alias names into a single search results, and keep API search name as primary name
			Collection<SearchResult> result = StreamEx.of(apiSearch.get()).append(localSearch.get()).groupingBy(SearchResult::getId, collectingAndThen(toList(), group -> merge(group.get(0), group))).values();

			return sortBySimilarity(result, singleton(query), getSeriesMatchMetric());
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
		private final Resource<LocalSearch<SubtitleSearchResult>> localIndex = Resource.lazy(() -> {
			return new LocalSearch<SubtitleSearchResult>(releaseInfo.getOpenSubtitlesIndex(), SearchResult::getEffectiveNames);
		}).memoize();

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
