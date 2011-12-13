
package net.sourceforge.filebot.web;


import static java.lang.Math.*;
import static net.sourceforge.filebot.web.OpenSubtitlesHasher.*;

import java.io.File;
import java.math.BigInteger;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
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

import redstone.xmlrpc.XmlRpcException;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.web.OpenSubtitlesXmlRpc.Query;
import net.sourceforge.tuned.Timer;


/**
 * SubtitleClient for OpenSubtitles.
 */
public class OpenSubtitlesClient implements SubtitleProvider, VideoHashSubtitleService, MovieIdentificationService {
	
	private final OpenSubtitlesXmlRpc xmlrpc;
	
	
	public OpenSubtitlesClient(String useragent) {
		this.xmlrpc = new OpenSubtitlesXmlRpc(useragent);
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
	
	
	@Override
	public List<SearchResult> search(String query) throws Exception {
		// require login
		login();
		
		try {
			// search for movies / series
			List<Movie> resultSet = xmlrpc.searchMoviesOnIMDB(query);
			return Arrays.asList(resultSet.toArray(new SearchResult[0]));
		} catch (ClassCastException e) {
			// unexpected xmlrpc responses (e.g. error messages instead of results) will trigger this
			throw new XmlRpcException("Illegal XMLRPC response on searchMoviesOnIMDB");
		}
	}
	
	
	@Override
	public List<SubtitleDescriptor> getSubtitleList(SearchResult searchResult, String languageName) throws Exception {
		// singleton array with or empty array
		int imdbid = ((Movie) searchResult).getImdbId();
		String[] languageFilter = languageName != null ? new String[] { getSubLanguageID(languageName) } : new String[0];
		
		// require login
		login();
		
		// get subtitle list
		SubtitleDescriptor[] subtitles = xmlrpc.searchSubtitles(imdbid, languageFilter).toArray(new SubtitleDescriptor[0]);
		
		return Arrays.asList(subtitles);
	}
	
	
	public Map<File, List<SubtitleDescriptor>> getSubtitleList(File[] files, String languageName) throws Exception {
		// singleton array with or empty array
		String[] languageFilter = languageName != null ? new String[] { getSubLanguageID(languageName) } : new String[0];
		
		// remember hash for each file
		Map<String, File> hashMap = new HashMap<String, File>(files.length);
		Map<File, List<SubtitleDescriptor>> resultMap = new HashMap<File, List<SubtitleDescriptor>>(files.length);
		
		// create hash query for each file
		List<Query> queryList = new ArrayList<Query>(files.length);
		
		for (File file : files) {
			// add query
			if (file.length() > HASH_CHUNK_SIZE) {
				String movieHash = computeHash(file);
				queryList.add(Query.forHash(movieHash, file.length(), languageFilter));
				hashMap.put(movieHash, file);
			}
			
			// prepare result map
			resultMap.put(file, new LinkedList<SubtitleDescriptor>());
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
					File file = hashMap.get(subtitle.getMovieHash());
					
					// add subtitle
					resultMap.get(file).add(subtitle);
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
	
	
	@Override
	public List<Movie> searchMovie(String query, Locale locale) throws Exception {
		// require login
		login();
		
		return xmlrpc.searchMoviesOnIMDB(query);
	}
	
	
	@Override
	public Movie getMovieDescriptor(int imdbid, Locale locale) throws Exception {
		// require login
		login();
		
		return xmlrpc.getIMDBMovieDetails(imdbid);
	}
	
	
	@Override
	public Movie[] getMovieDescriptors(File[] movieFiles, Locale locale) throws Exception {
		// create result array
		Movie[] result = new Movie[movieFiles.length];
		
		// compute movie hashes
		Map<String, Integer> indexMap = new HashMap<String, Integer>(movieFiles.length);
		
		for (int i = 0; i < movieFiles.length; i++) {
			if (movieFiles[i].length() > HASH_CHUNK_SIZE) {
				indexMap.put(computeHash(movieFiles[i]), i); // remember original index
			}
		}
		
		if (indexMap.size() > 0) {
			// require login
			login();
			
			// dispatch query for all hashes
			List<String> hashes = new ArrayList<String>(indexMap.keySet());
			int batchSize = 50;
			for (int bn = 0; bn < ceil((float) hashes.size() / batchSize); bn++) {
				List<String> batch = hashes.subList(bn * batchSize, min((bn * batchSize) + batchSize, hashes.size()));
				
				for (Entry<String, Movie> entry : xmlrpc.checkMovieHash(batch).entrySet()) {
					int index = indexMap.get(entry.getKey());
					result[index] = entry.getValue();
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
			xmlrpc.loginAnonymous();
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
		Cache cache = CacheManager.getInstance().getCache("web-persistent-datasource");
		String key = getClass().getName() + ".getSubLanguageMap";
		
		Element element = cache.get(key);
		Map<String, String> subLanguageMap;
		
		if (element == null) {
			subLanguageMap = new HashMap<String, String>();
			
			// fetch language data
			for (Entry<String, String> entry : xmlrpc.getSubLanguages().entrySet()) {
				// map id by name
				subLanguageMap.put(entry.getValue().toLowerCase(), entry.getKey().toLowerCase());
			}
			
			// some additional special handling
			subLanguageMap.put("brazilian", "pob");
			
			// cache data
			cache.put(new Element(key, subLanguageMap));
		} else {
			// use cached entry
			subLanguageMap = (Map<String, String>) element.getValue();
		}
		
		return subLanguageMap;
	}
	
	
	protected String getSubLanguageID(String languageName) throws Exception {
		if (!getSubLanguageMap().containsKey(languageName.toLowerCase())) {
			throw new IllegalArgumentException(String.format("SubLanguageID for '%s' not found", languageName));
		}
		
		return getSubLanguageMap().get(languageName.toLowerCase());
	}
	
	
	protected String getLanguageName(String subLanguageID) throws Exception {
		for (Entry<String, String> it : getSubLanguageMap().entrySet()) {
			if (it.getValue().equals(subLanguageID.toLowerCase()))
				return it.getKey();
		}
		
		return null;
	}
	
}
