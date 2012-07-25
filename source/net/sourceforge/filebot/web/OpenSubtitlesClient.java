
package net.sourceforge.filebot.web;


import static java.lang.Math.*;
import static java.util.Arrays.*;
import static java.util.Collections.*;
import static net.sourceforge.filebot.web.OpenSubtitlesHasher.*;

import java.io.File;
import java.math.BigInteger;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Icon;

import net.sourceforge.filebot.Cache;
import net.sourceforge.filebot.Cache.Key;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.web.OpenSubtitlesXmlRpc.Query;
import net.sourceforge.tuned.Timer;
import redstone.xmlrpc.XmlRpcException;


/**
 * SubtitleClient for OpenSubtitles.
 */
public class OpenSubtitlesClient implements SubtitleProvider, VideoHashSubtitleService, MovieIdentificationService {
	
	private final OpenSubtitlesXmlRpc xmlrpc;
	
	private String username = "";
	private String password = "";
	
	
	public OpenSubtitlesClient(String useragent) {
		this.xmlrpc = new OpenSubtitlesXmlRpc(useragent);
	}
	
	
	public void setUser(String username, String password) {
		this.username = username;
		this.password = password;
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
		return new ResultCache("opensubtitles.org", Cache.getCache("web-datasource"));
	}
	
	
	@Override
	public List<SearchResult> search(String query) throws Exception {
		List<SearchResult> result = getCache().getSearchResult("search", query, null);
		if (result != null) {
			return result;
		}
		
		// require login
		login();
		
		try {
			// search for movies / series
			List<Movie> resultSet = xmlrpc.searchMoviesOnIMDB(query);
			result = asList(resultSet.toArray(new SearchResult[0]));
		} catch (ClassCastException e) {
			// unexpected xmlrpc responses (e.g. error messages instead of results) will trigger this
			throw new XmlRpcException("Illegal XMLRPC response on searchMoviesOnIMDB");
		}
		
		getCache().putSearchResult("search", query, null, result);
		return result;
	}
	
	
	@Override
	public List<SubtitleDescriptor> getSubtitleList(SearchResult searchResult, String languageName) throws Exception {
		List<SubtitleDescriptor> subtitles = getCache().getSubtitleDescriptorList(searchResult, languageName);
		if (subtitles != null) {
			return subtitles;
		}
		
		// singleton array with or empty array
		int imdbid = ((Movie) searchResult).getImdbId();
		String[] languageFilter = languageName != null ? new String[] { getSubLanguageID(languageName) } : new String[0];
		
		// require login
		login();
		
		// get subtitle list
		subtitles = asList(xmlrpc.searchSubtitles(imdbid, languageFilter).toArray(new SubtitleDescriptor[0]));
		
		getCache().putSubtitleDescriptorList(searchResult, languageName, subtitles);
		return subtitles;
	}
	
	
	@Override
	public Map<File, List<SubtitleDescriptor>> getSubtitleList(File[] files, String languageName) throws Exception {
		// singleton array with or empty array
		String[] languageFilter = languageName != null ? new String[] { getSubLanguageID(languageName) } : new String[0];
		
		// remember hash for each file
		Map<Query, File> hashMap = new HashMap<Query, File>(files.length);
		Map<File, List<SubtitleDescriptor>> resultMap = new HashMap<File, List<SubtitleDescriptor>>(files.length);
		
		// create hash query for each file
		List<Query> queryList = new ArrayList<Query>(files.length);
		
		for (File file : files) {
			// add query
			if (file.length() > HASH_CHUNK_SIZE) {
				String movieHash = computeHash(file);
				Query query = Query.forHash(movieHash, file.length(), languageFilter);
				
				// check hash
				List<SubtitleDescriptor> cachedResults = getCache().getSubtitleDescriptorList(query, languageName);
				if (cachedResults == null) {
					queryList.add(query);
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
		
		if (queryList.size() > 0) {
			// require login
			login();
			
			// dispatch query for all hashes
			int batchSize = 50;
			for (int bn = 0; bn < ceil((float) queryList.size() / batchSize); bn++) {
				List<Query> batch = queryList.subList(bn * batchSize, min((bn * batchSize) + batchSize, queryList.size()));
				
				// submit query and map results to given files
				for (OpenSubtitlesSubtitleDescriptor subtitle : xmlrpc.searchSubtitles(batch)) {
					// get file for hash
					File file = hashMap.get(Query.forHash(subtitle.getMovieHash(), subtitle.getMovieByteSize(), languageFilter));
					
					// add subtitle
					resultMap.get(file).add(subtitle);
				}
				
				for (Query query : batch) {
					getCache().putSubtitleDescriptorList(query, languageName, resultMap.get(hashMap.get(query)));
				}
			}
		}
		
		return resultMap;
	}
	
	
	@Override
	public boolean publishSubtitle(int imdbid, String languageName, File videoFile, File subtitleFile) throws Exception {
		//TODO implement upload feature
		return false;
	}
	
	
	/**
	 * Calculate MD5 hash.
	 */
	private String md5(byte[] data) {
		try {
			MessageDigest hash = MessageDigest.getInstance("MD5");
			hash.update(data);
			return String.format("%032x", new BigInteger(1, hash.digest())); // as hex string
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e); // won't happen
		}
	}
	
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public List<Movie> searchMovie(String query, Locale locale) throws Exception {
		List<SearchResult> result = getCache().getSearchResult("searchMovie", query, locale);
		if (result != null) {
			return (List) result;
		}
		
		// require login
		login();
		
		List<Movie> movies = xmlrpc.searchMoviesOnIMDB(query);
		
		getCache().putSearchResult("searchMovie", query, locale, movies);
		return movies;
	}
	
	
	@Override
	public Movie getMovieDescriptor(int imdbid, Locale locale) throws Exception {
		Movie result = getCache().getData("getMovieDescriptor", imdbid, locale, Movie.class);
		if (result != null) {
			return result;
		}
		
		// require login
		login();
		
		Movie movie = xmlrpc.getIMDBMovieDetails(imdbid);
		
		getCache().putData("getMovieDescriptor", imdbid, locale, movie);
		return movie;
	}
	
	
	public Movie getMovieDescriptor(File movieFile, Locale locale) throws Exception {
		return getMovieDescriptors(singleton(movieFile), locale).get(movieFile);
	}
	
	
	@Override
	public Map<File, Movie> getMovieDescriptors(Collection<File> movieFiles, Locale locale) throws Exception {
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
				} else {
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
				
				for (Entry<String, Movie> it : xmlrpc.checkMovieHash(batch).entrySet()) {
					result.put(hashMap.get(it.getKey()), it.getValue());
					getCache().putData("getMovieDescriptor", it.getKey(), locale, it.getValue());
				}
			}
		}
		
		return result;
	}
	
	
	@Override
	public URI getSubtitleListLink(SearchResult searchResult, String languageName) {
		Movie movie = (Movie) searchResult;
		String sublanguageid = "all";
		
		if (languageName != null) {
			try {
				sublanguageid = getSubLanguageID(languageName);
			} catch (Exception e) {
				Logger.getLogger(getClass().getName()).log(Level.WARNING, e.getMessage(), e);
			}
		}
		
		return URI.create(String.format("http://www.opensubtitles.org/en/search/imdbid-%d/sublanguageid-%s", movie.getImdbId(), sublanguageid));
	}
	
	
	public Locale detectLanguage(byte[] data) throws Exception {
		// require login
		login();
		
		// detect language
		List<String> languages = xmlrpc.detectLanguage(data);
		
		// return first language
		return languages.size() > 0 ? new Locale(languages.get(0)) : null;
	}
	
	
	protected synchronized void login() throws Exception {
		if (!xmlrpc.isLoggedOn()) {
			xmlrpc.login(username, password, "en");
		}
		
		logoutTimer.set(10, TimeUnit.MINUTES, true);
	}
	
	
	protected synchronized void logout() {
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
	
	
	/**
	 * SubLanguageID by English language name
	 */
	@SuppressWarnings("unchecked")
	protected synchronized Map<String, String> getSubLanguageMap() throws Exception {
		Cache cache = Cache.getCache("web-persistent-datasource");
		String cacheKey = getClass().getName() + ".subLanguageMap";
		
		Map<String, String> subLanguageMap = cache.get(cacheKey, Map.class);
		
		if (subLanguageMap == null) {
			subLanguageMap = new HashMap<String, String>();
			
			// fetch language data
			for (Entry<String, String> entry : xmlrpc.getSubLanguages().entrySet()) {
				// map id by name
				subLanguageMap.put(entry.getValue().toLowerCase(), entry.getKey().toLowerCase());
			}
			
			// some additional special handling
			subLanguageMap.put("brazilian", "pob");
			
			// cache data
			cache.put(cacheKey, subLanguageMap);
		}
		
		return subLanguageMap;
	}
	
	
	protected String getSubLanguageID(String languageName) throws Exception {
		Map<String, String> subLanguageMap = getSubLanguageMap();
		String key = languageName.toLowerCase();
		
		if (!subLanguageMap.containsKey(key)) {
			throw new IllegalArgumentException(String.format("SubLanguageID for '%s' not found", key));
		}
		
		return subLanguageMap.get(key);
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
		
		
		public <T extends SearchResult> List<T> putSearchResult(String method, String query, Locale locale, List<T> value) {
			try {
				cache.put(new Key(id, normalize(query)), value.toArray(new SearchResult[0]));
			} catch (Exception e) {
				Logger.getLogger(OpenSubtitlesClient.class.getName()).log(Level.WARNING, e.getMessage());
			}
			
			return value;
		}
		
		
		@SuppressWarnings("unchecked")
		public List<SearchResult> getSearchResult(String method, String query, Locale locale) {
			try {
				SearchResult[] array = cache.get(new Key(id, normalize(query)), SearchResult[].class);
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
	
}
