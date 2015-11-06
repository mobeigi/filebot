package net.filebot.web;

import static java.lang.Math.*;
import static java.util.Arrays.*;
import static java.util.Collections.*;
import static net.filebot.util.FileUtilities.*;
import static net.filebot.web.OpenSubtitlesHasher.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Icon;

import net.filebot.Cache;
import net.filebot.Cache.Key;
import net.filebot.ResourceManager;
import net.filebot.media.MediaDetection;
import net.filebot.mediainfo.MediaInfo;
import net.filebot.mediainfo.MediaInfo.StreamKind;
import net.filebot.util.ExceptionUtilities;
import net.filebot.util.Timer;
import net.filebot.web.OpenSubtitlesXmlRpc.BaseInfo;
import net.filebot.web.OpenSubtitlesXmlRpc.Query;
import net.filebot.web.OpenSubtitlesXmlRpc.SubFile;
import net.filebot.web.OpenSubtitlesXmlRpc.TryUploadResponse;
import redstone.xmlrpc.XmlRpcException;
import redstone.xmlrpc.XmlRpcFault;

/**
 * SubtitleClient for OpenSubtitles.
 */
public class OpenSubtitlesClient implements SubtitleProvider, VideoHashSubtitleService, MovieIdentificationService {

	public final OpenSubtitlesXmlRpc xmlrpc;

	private String username = "";
	private String password = "";

	public OpenSubtitlesClient(String name, String version) {
		this.xmlrpc = new OpenSubtitlesXmlRpcWithRetryAndFloodLimit(String.format("%s v%s", name, version), 2, 3000);
	}

	public synchronized void setUser(String username, String password_md5) {
		// cancel previous session
		this.logout();

		this.username = username;
		this.password = password_md5;
	}

	public boolean isAnonymous() {
		return username == null || username.isEmpty();
	}

	@Override
	public String getName() {
		return "OpenSubtitles";
	}

	@Override
	public URI getLink() {
		return URI.create("http://www.opensubtitles.org");
	}

	@Override
	public Icon getIcon() {
		return ResourceManager.getIcon("search.opensubtitles");
	}

	public ResultCache getCache() {
		return new ResultCache(getName(), Cache.getCache("web-datasource"));
	}

	@Override
	public synchronized List<SubtitleSearchResult> search(String query) throws Exception {
		throw new UnsupportedOperationException(); // XMLRPC::SearchMoviesOnIMDB is not allowed due to abuse
	}

	@Override
	public List<SubtitleSearchResult> guess(String tag) throws Exception {
		List<SubtitleSearchResult> subtitles = getCache().getSearchResult("guess", tag);
		if (subtitles != null) {
			return subtitles;
		}

		// require login
		login();

		subtitles = xmlrpc.guessMovie(singleton(tag)).getOrDefault(tag, emptyList());

		getCache().putSearchResult("guess", tag, subtitles);
		return subtitles;
	}

	@Override
	public synchronized List<SubtitleDescriptor> getSubtitleList(SubtitleSearchResult searchResult, String languageName) throws Exception {
		List<SubtitleDescriptor> subtitles = getCache().getSubtitleDescriptorList(searchResult, languageName);
		if (subtitles != null) {
			return subtitles;
		}

		// singleton array with or empty array
		int imdbid = ((Movie) searchResult).getImdbId();
		String[] languageFilter = languageName != null ? new String[] { getSubLanguageID(languageName, true) } : new String[0];

		// require login
		login();

		// get subtitle list
		subtitles = asList(xmlrpc.searchSubtitles(imdbid, languageFilter).toArray(new SubtitleDescriptor[0]));

		getCache().putSubtitleDescriptorList(searchResult, languageName, subtitles);
		return subtitles;
	}

	@Override
	public Map<File, List<SubtitleDescriptor>> getSubtitleList(File[] files, String languageName) throws Exception {
		Map<File, List<SubtitleDescriptor>> results = new HashMap<File, List<SubtitleDescriptor>>(files.length);
		Set<File> remainingFiles = new LinkedHashSet<File>(asList(files));

		// lookup subtitles by hash
		if (remainingFiles.size() > 0) {
			results.putAll(getSubtitleListByHash(remainingFiles.toArray(new File[0]), languageName));
		}

		for (Entry<File, List<SubtitleDescriptor>> it : results.entrySet()) {
			if (it.getValue().size() > 0) {
				remainingFiles.remove(it.getKey());
			}
		}

		// lookup subtitles by tag
		if (remainingFiles.size() > 0) {
			results.putAll(getSubtitleListByTag(remainingFiles.toArray(new File[0]), languageName));
		}

		return results;
	}

	// max numbers of queries to submit in a single XML-RPC request, but currently only batchSize == 1 is supported
	private final int batchSize = 1;

	public synchronized Map<File, List<SubtitleDescriptor>> getSubtitleListByHash(File[] files, String languageName) throws Exception {
		// singleton array with or empty array
		String[] languageFilter = languageName != null ? new String[] { getSubLanguageID(languageName, true) } : new String[0];

		// remember hash for each file
		Map<Query, File> hashMap = new HashMap<Query, File>(files.length);
		Map<File, List<SubtitleDescriptor>> resultMap = new HashMap<File, List<SubtitleDescriptor>>(files.length);

		// create hash query for each file
		List<Query> hashQueryList = new ArrayList<Query>(files.length);

		for (File file : files) {
			// add hash query
			if (file.length() > HASH_CHUNK_SIZE) {
				String movieHash = computeHash(file);
				Query query = Query.forHash(movieHash, file.length(), languageFilter);

				// check hash
				List<SubtitleDescriptor> cachedResults = getCache().getSubtitleDescriptorList(query, languageName);
				if (cachedResults == null) {
					hashQueryList.add(query);
					hashMap.put(query, file);
				} else {
					resultMap.put(file, cachedResults);
				}
			}

			// prepare result map
			if (resultMap.get(file) == null) {
				resultMap.put(file, new LinkedList<SubtitleDescriptor>());
			}
		}

		if (hashQueryList.size() > 0) {
			// require login
			login();

			// dispatch query for all hashes
			for (int bn = 0; bn < ceil((float) hashQueryList.size() / batchSize); bn++) {
				List<Query> batch = hashQueryList.subList(bn * batchSize, min((bn * batchSize) + batchSize, hashQueryList.size()));

				// submit query and map results to given files
				for (OpenSubtitlesSubtitleDescriptor subtitle : xmlrpc.searchSubtitles(batch)) {
					// get file for hash
					File file = hashMap.get((batch.get(0)));

					// add subtitle
					if (file != null) {
						resultMap.get(file).add(subtitle);
					} else {
						Logger.getLogger(getClass().getName()).log(Level.WARNING, "Unable to map hash to file: " + subtitle.getMovieHash());
					}
				}

				for (Query query : batch) {
					getCache().putSubtitleDescriptorList(query, languageName, resultMap.get(hashMap.get(query)));
				}
			}
		}

		return resultMap;
	}

	public synchronized Map<File, List<SubtitleDescriptor>> getSubtitleListByTag(File[] files, String languageName) throws Exception {
		// singleton array with or empty array
		String[] languageFilter = languageName != null ? new String[] { getSubLanguageID(languageName, true) } : new String[0];

		// remember tag for each file
		Map<Query, File> tagMap = new HashMap<Query, File>(files.length);
		Map<File, List<SubtitleDescriptor>> resultMap = new HashMap<File, List<SubtitleDescriptor>>(files.length);

		// create tag query for each file
		List<Query> tagQueryList = new ArrayList<Query>(files.length);

		for (File file : files) {
			// add tag query
			String tag = getNameWithoutExtension(file.getName());
			Query query = Query.forTag(tag, languageFilter);

			// check tag
			List<SubtitleDescriptor> cachedResults = getCache().getSubtitleDescriptorList(query, languageName);
			if (cachedResults == null) {
				tagQueryList.add(query);
				tagMap.put(query, file);
			} else {
				resultMap.put(file, cachedResults);
			}

			// prepare result map
			if (resultMap.get(file) == null) {
				resultMap.put(file, new LinkedList<SubtitleDescriptor>());
			}
		}

		if (tagQueryList.size() > 0) {
			// require login
			login();

			// dispatch query for all hashes
			for (int bn = 0; bn < ceil((float) tagQueryList.size() / batchSize); bn++) {
				List<Query> batch = tagQueryList.subList(bn * batchSize, min((bn * batchSize) + batchSize, tagQueryList.size()));

				// submit query and map results to given files
				for (OpenSubtitlesSubtitleDescriptor subtitle : xmlrpc.searchSubtitles(batch)) {
					// get file for tag
					File file = tagMap.get(batch.get(0));

					// add subtitle
					if (file != null) {
						resultMap.get(file).add(subtitle);
					} else {
						Logger.getLogger(getClass().getName()).log(Level.WARNING, "Unable to map release name to file: " + subtitle.getMovieReleaseName());
					}
				}

				for (Query query : batch) {
					getCache().putSubtitleDescriptorList(query, languageName, resultMap.get(tagMap.get(query)));
				}
			}
		}

		return resultMap;
	}

	@Override
	public CheckResult checkSubtitle(File videoFile, File subtitleFile) throws Exception {
		// subhash (md5 of subtitles), subfilename, moviehash, moviebytesize, moviefilename
		SubFile sub = new SubFile();
		sub.setSubHash(md5(readFile(subtitleFile)));
		sub.setSubFileName(subtitleFile.getName());
		sub.setMovieHash(computeHash(videoFile));
		sub.setMovieByteSize(videoFile.length());
		sub.setMovieFileName(videoFile.getName());

		// require login
		login();

		// check if subs already exist in DB
		TryUploadResponse response = xmlrpc.tryUploadSubtitles(sub);

		// TryUploadResponse: false => [{HashWasAlreadyInDb=1, MovieKind=movie, IDSubtitle=3167446, MoviefilenameWasAlreadyInDb=1, ISO639=en, MovieYear=2007, SubLanguageID=eng, MovieName=Blades of Glory, MovieNameEng=, IDMovieImdb=445934}]
		boolean exists = !response.isUploadRequired();
		Movie identity = null;
		Locale language = null;

		if (response.getSubtitleData().size() > 0) {
			try {
				Map<String, String> fields = response.getSubtitleData().get(0);

				String lang = fields.get("SubLanguageID");
				language = new Locale(lang);

				String imdb = fields.get("IDMovieImdb");
				String name = fields.get("MovieName");
				String year = fields.get("MovieYear");
				identity = new Movie(name, Integer.parseInt(year), Integer.parseInt(imdb), -1);
			} catch (Exception e) {
				Logger.getLogger(getClass().getName()).log(Level.WARNING, e.getMessage());
			}
		}

		return new CheckResult(exists, identity, language);

	}

	@Override
	public void uploadSubtitle(Object identity, Locale language, File videoFile, File subtitleFile) throws Exception {
		if (!(identity instanceof Movie && ((Movie) identity).getImdbId() > 0)) {
			throw new IllegalArgumentException("Illegal Movie ID: " + identity);
		}

		int imdbid = ((Movie) identity).getImdbId();
		String languageName = getSubLanguageID(language.getDisplayName(Locale.ENGLISH), false);

		// subhash (md5 of subtitles), subfilename, moviehash, moviebytesize, moviefilename
		SubFile sub = new SubFile();
		sub.setSubHash(md5(readFile(subtitleFile)));
		sub.setSubFileName(subtitleFile.getName());
		sub.setMovieHash(computeHash(videoFile));
		sub.setMovieByteSize(videoFile.length());
		sub.setMovieFileName(videoFile.getName());

		BaseInfo info = new BaseInfo();
		info.setIDMovieImdb(imdbid);
		info.setSubLanguageID(languageName);

		// encode subtitle contents
		sub.setSubContent(readFile(subtitleFile));

		try {
			MediaInfo mi = new MediaInfo();
			mi.open(videoFile);
			sub.setMovieFPS(mi.get(StreamKind.Video, 0, "FrameRate"));
			sub.setMovieTimeMS(mi.get(StreamKind.General, 0, "Duration"));
			mi.close();
		} catch (Throwable e) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, e.getMessage(), e);
		}

		// require login
		login();

		xmlrpc.uploadSubtitles(info, sub);
	}

	@Override
	public List<Movie> searchMovie(String query, Locale locale) throws Exception {
		throw new UnsupportedOperationException();
	}

	public synchronized List<SubtitleSearchResult> searchIMDB(String query) throws Exception {
		// search for movies and series
		List<SubtitleSearchResult> result = getCache().getSearchResult("search", query);
		if (result != null) {
			return result;
		}

		// require login
		login();

		try {
			// search for movies / series
			result = xmlrpc.searchMoviesOnIMDB(query);
		} catch (ClassCastException e) {
			// unexpected xmlrpc responses (e.g. error messages instead of results) will trigger this
			throw new XmlRpcException("Illegal XMLRPC response on searchMoviesOnIMDB");
		}

		getCache().putSearchResult("search", query, result);
		return result;
	}

	@Override
	public synchronized Movie getMovieDescriptor(Movie id, Locale locale) throws Exception {
		if (id.getImdbId() <= 0) {
			throw new IllegalArgumentException("id must not be " + id.getImdbId());
		}

		Movie result = getCache().getData("getMovieDescriptor", id.getImdbId(), locale, Movie.class);
		if (result != null) {
			return result;
		}

		// require login
		login();

		Movie movie = xmlrpc.getIMDBMovieDetails(id.getImdbId());

		getCache().putData("getMovieDescriptor", id.getImdbId(), locale, movie);
		return movie;
	}

	public Movie getMovieDescriptor(File movieFile, Locale locale) throws Exception {
		return getMovieDescriptors(singleton(movieFile), locale).get(movieFile);
	}

	@Override
	public synchronized Map<File, Movie> getMovieDescriptors(Collection<File> movieFiles, Locale locale) throws Exception {
		// create result array
		Map<File, Movie> result = new HashMap<File, Movie>();

		// compute movie hashes
		Map<String, File> hashMap = new HashMap<String, File>(movieFiles.size());

		for (File file : movieFiles) {
			if (file.length() > HASH_CHUNK_SIZE) {
				String hash = computeHash(file);

				Movie entry = getCache().getData("getMovieDescriptor", hash, locale, Movie.class);
				if (entry == null) {
					hashMap.put(hash, file); // map file by hash
				} else if (entry.getName().length() > 0) {
					result.put(file, entry);
				}
			}
		}

		if (hashMap.size() > 0) {
			// require login
			login();

			// dispatch query for all hashes
			List<String> hashes = new ArrayList<String>(hashMap.keySet());
			int batchSize = 50;
			for (int bn = 0; bn < ceil((float) hashes.size() / batchSize); bn++) {
				List<String> batch = hashes.subList(bn * batchSize, min((bn * batchSize) + batchSize, hashes.size()));
				Set<String> unmatchedHashes = new HashSet<String>(batch);

				int minSeenCount = 20; // make sure we don't get mismatches by making sure the hash has not been confirmed numerous times
				for (Entry<String, Movie> it : xmlrpc.checkMovieHash(batch, minSeenCount).entrySet()) {
					String hash = it.getKey();
					Movie movie = it.getValue();

					result.put(hashMap.get(hash), movie);
					getCache().putData("getMovieDescriptor", hash, locale, movie);

					unmatchedHashes.remove(hash);
				}

				// note hashes that are not matched to any items so we can ignore them in the future
				for (String hash : unmatchedHashes) {
					getCache().putData("getMovieDescriptor", hash, locale, new Movie("", -1, -1, -1));
				}
			}

		}

		return result;
	}

	@Override
	public URI getSubtitleListLink(SubtitleSearchResult searchResult, String languageName) {
		Movie movie = searchResult;
		String sublanguageid = "all";

		if (languageName != null) {
			try {
				sublanguageid = getSubLanguageID(languageName, true);
			} catch (Exception e) {
				Logger.getLogger(getClass().getName()).log(Level.WARNING, e.getMessage(), e);
			}
		}

		return URI.create(String.format("http://www.opensubtitles.org/en/search/imdbid-%d/sublanguageid-%s", movie.getImdbId(), sublanguageid));
	}

	public synchronized Locale detectLanguage(byte[] data) throws Exception {
		if (data.length < 256) {
			throw new IllegalArgumentException("data is not enough");
		}

		String language = getCache().getData("detectLanguage", md5(data), Locale.ROOT, String.class);
		if (language != null) {
			return language.isEmpty() ? null : new Locale(language);
		}

		// require login
		login();

		// detect language
		List<String> languages = xmlrpc.detectLanguage(data);

		// return first language
		language = languages.size() > 0 ? languages.get(0) : "";
		getCache().putData("detectLanguage", md5(data), Locale.ROOT, language);
		return new Locale(language);
	}

	public synchronized void login() throws Exception {
		if (!xmlrpc.isLoggedOn()) {
			xmlrpc.login(username, password, "en");
		}

		logoutTimer.set(10, TimeUnit.MINUTES, true);
	}

	public synchronized void logout() {
		if (xmlrpc.isLoggedOn()) {
			try {
				xmlrpc.logout();
			} catch (Exception e) {
				Logger.getLogger(getClass().getName()).log(Level.WARNING, "Logout failed", e);
			}
		}
		logoutTimer.cancel();
	}

	protected final Timer logoutTimer = new Timer() {

		@Override
		public void run() {
			logout();
		}
	};

	public Map<?, ?> getServerInfo() throws Exception {
		// require login
		login();

		return xmlrpc.getServerInfo();
	}

	public Map<?, ?> getDownloadLimits() throws Exception {
		return (Map<?, ?>) getServerInfo().get("download_limits");
	}

	/**
	 * SubLanguageID by English language name
	 */
	@SuppressWarnings("unchecked")
	protected synchronized Map<String, String> getSubLanguageMap() throws Exception {
		Cache cache = Cache.getCache("web-datasource-lv2");
		String cacheKey = getClass().getName() + ".subLanguageMap";

		// try to get language map from cache
		Map<String, String> subLanguageMap = cache.get(cacheKey, Map.class);

		if (subLanguageMap == null) {
			subLanguageMap = new HashMap<String, String>();

			// add additional language aliases for improved compatibility
			Map<String, Locale> additionalLanguageMappings = MediaDetection.releaseInfo.getLanguageMap(Locale.ENGLISH);

			// fetch language data
			for (Entry<String, String> entry : xmlrpc.getSubLanguages().entrySet()) {
				// map id by name
				String subLanguageID = entry.getKey().toLowerCase();
				String subLanguageName = entry.getValue().toLowerCase();

				subLanguageMap.put(subLanguageName, subLanguageID);
				subLanguageMap.put(subLanguageID, subLanguageID); // add reverse mapping as well for improved compatibility

				// add additional language aliases for improved compatibility
				for (String key : asList(subLanguageID, subLanguageName)) {
					Locale locale = additionalLanguageMappings.get(key);
					if (locale != null) {
						for (String identifier : asList(locale.getLanguage(), locale.getISO3Language(), locale.getDisplayLanguage(Locale.ENGLISH))) {
							if (identifier != null && identifier.length() > 0 && !subLanguageMap.containsKey(identifier.toLowerCase())) {
								subLanguageMap.put(identifier.toLowerCase(), subLanguageID);
							}
						}
					}
				}
			}

			// some additional special handling
			subLanguageMap.put("brazilian", "pob");
			subLanguageMap.put("chinese", "chi,zht,zhe"); // Chinese (Simplified) / Chinese (Traditional) / Chinese (bilingual)

			// cache data
			cache.put(cacheKey, subLanguageMap);
		}

		return subLanguageMap;
	}

	protected String getSubLanguageID(String languageName, boolean allowMultiLanguageID) throws Exception {
		String subLanguageID = getSubLanguageMap().get(languageName.toLowerCase());
		if (subLanguageID == null) {
			throw new IllegalArgumentException(String.format("SubLanguageID for '%s' not found", languageName));
		}
		if (!allowMultiLanguageID && subLanguageID.contains(",")) {
			subLanguageID = subLanguageID.substring(0, subLanguageID.indexOf(","));
		}
		return subLanguageID;
	}

	protected String getLanguageName(String subLanguageID) throws Exception {
		for (Entry<String, String> it : getSubLanguageMap().entrySet()) {
			if (it.getValue().equals(subLanguageID.toLowerCase()))
				return it.getKey();
		}

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

		public <T extends SubtitleSearchResult> List<T> putSearchResult(String method, String query, List<T> value) {
			try {
				cache.put(new Key(id, method, normalize(query)), value.toArray(new SubtitleSearchResult[0]));
			} catch (Exception e) {
				Logger.getLogger(OpenSubtitlesClient.class.getName()).log(Level.WARNING, e.getMessage());
			}

			return value;
		}

		@SuppressWarnings("unchecked")
		public List<SubtitleSearchResult> getSearchResult(String method, String query) {
			try {
				SubtitleSearchResult[] array = cache.get(new Key(id, method, normalize(query)), SubtitleSearchResult[].class);
				if (array != null) {
					return Arrays.asList(array);
				}
			} catch (Exception e) {
				Logger.getLogger(OpenSubtitlesClient.class.getName()).log(Level.WARNING, e.getMessage(), e);
			}

			return null;
		}

		public List<SubtitleDescriptor> putSubtitleDescriptorList(Object key, String locale, List<SubtitleDescriptor> subtitles) {
			try {
				cache.put(new Key(id, key, locale), subtitles.toArray(new SubtitleDescriptor[0]));
			} catch (Exception e) {
				Logger.getLogger(OpenSubtitlesClient.class.getName()).log(Level.WARNING, e.getMessage());
			}

			return subtitles;
		}

		public List<SubtitleDescriptor> getSubtitleDescriptorList(Object key, String locale) {
			try {
				SubtitleDescriptor[] descriptors = cache.get(new Key(id, key, locale), SubtitleDescriptor[].class);
				if (descriptors != null) {
					return Arrays.asList(descriptors);
				}
			} catch (Exception e) {
				Logger.getLogger(OpenSubtitlesClient.class.getName()).log(Level.WARNING, e.getMessage(), e);
			}

			return null;
		}

		public void putData(Object category, Object key, Locale locale, Object object) {
			try {
				cache.put(new Key(id, category, locale, key), object);
			} catch (Exception e) {
				Logger.getLogger(OpenSubtitlesClient.class.getName()).log(Level.WARNING, e.getMessage());
			}
		}

		public <T> T getData(Object category, Object key, Locale locale, Class<T> type) {
			try {
				T value = cache.get(new Key(id, category, locale, key), type);
				if (value != null) {
					return value;
				}
			} catch (Exception e) {
				Logger.getLogger(OpenSubtitlesClient.class.getName()).log(Level.WARNING, e.getMessage(), e);
			}

			return null;
		}
	}

	protected static class OpenSubtitlesXmlRpcWithRetryAndFloodLimit extends OpenSubtitlesXmlRpc {

		private final Object lock = new Object();

		private int retryCountLimit;
		private long retryWaitTime;

		public OpenSubtitlesXmlRpcWithRetryAndFloodLimit(String useragent, int retryCountLimit, long retryWaitTime) {
			super(useragent);
			this.retryCountLimit = retryCountLimit;
			this.retryWaitTime = retryWaitTime;
		}

		@Override
		protected Map<?, ?> invoke(String method, Object... arguments) throws XmlRpcFault {
			for (int i = 0; retryCountLimit < 0 || i <= retryCountLimit; i++) {
				try {
					if (i > 0) {
						Thread.sleep(retryWaitTime);
					}

					// only allow 1 single concurrent connection at any time (to reduce abuse)
					synchronized (lock) {
						return super.invoke(method, arguments);
					}
				} catch (XmlRpcException e) {
					IOException ioException = ExceptionUtilities.findCause(e, IOException.class);
					if (ioException == null || i >= 0 && i >= retryCountLimit) {
						throw e;
					}
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
			return null; // can't happen
		}

	}

}
