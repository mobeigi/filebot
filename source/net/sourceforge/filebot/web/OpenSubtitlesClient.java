
package net.sourceforge.filebot.web;


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
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Icon;

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
		
		// search for movies / series
		SearchResult[] result = xmlrpc.searchMoviesOnIMDB(query).toArray(new SearchResult[0]);
		
		return Arrays.asList(result);
	}
	

	@Override
	public List<SubtitleDescriptor> getSubtitleList(SearchResult searchResult, String languageName) throws Exception {
		// singleton array with or empty array
		int imdbid = ((MovieDescriptor) searchResult).getImdbId();
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
			String movieHash = OpenSubtitlesHasher.computeHash(file);
			
			// add query
			queryList.add(Query.forHash(movieHash, file.length(), languageFilter));
			
			// prepare result map
			hashMap.put(movieHash, file);
			resultMap.put(file, new LinkedList<SubtitleDescriptor>());
		}
		
		// require login
		login();
		
		// submit query and map results to given files
		for (OpenSubtitlesSubtitleDescriptor subtitle : xmlrpc.searchSubtitles(queryList)) {
			// get file for hash
			File file = hashMap.get(subtitle.getMovieHash());
			
			// add subtitle
			resultMap.get(file).add(subtitle);
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
			
			// return hex string
			return String.format("%032x", new BigInteger(1, hash.digest()));
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}
	

	@Override
	public List<MovieDescriptor> searchMovie(String query, Locale locale) throws Exception {
		// require login
		login();
		
		return xmlrpc.searchMoviesOnIMDB(query);
	}
	

	@Override
	public MovieDescriptor getMovieDescriptor(int imdbid, Locale locale) throws Exception {
		// require login
		login();
		
		return xmlrpc.getIMDBMovieDetails(imdbid);
	}
	

	@Override
	public MovieDescriptor[] getMovieDescriptors(File[] movieFiles, Locale locale) throws Exception {
		// create result array
		MovieDescriptor[] result = new MovieDescriptor[movieFiles.length];
		
		// compute movie hashes
		Map<String, Integer> indexMap = new HashMap<String, Integer>(movieFiles.length);
		
		for (int i = 0; i < movieFiles.length; i++) {
			String hash = OpenSubtitlesHasher.computeHash(movieFiles[i]);
			
			// remember original index
			indexMap.put(hash, i);
		}
		
		// require login
		login();
		
		// dispatch single query for all hashes
		for (Entry<String, MovieDescriptor> entry : xmlrpc.checkMovieHash(indexMap.keySet()).entrySet()) {
			int index = indexMap.get(entry.getKey());
			result[index] = entry.getValue();
		}
		
		return result;
	}
	

	@Override
	public URI getSubtitleListLink(SearchResult searchResult, String languageName) {
		MovieDescriptor movie = (MovieDescriptor) searchResult;
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
	private static final Map<String, String> subLanguageCache = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
	

	private String getSubLanguageID(String languageName) throws Exception {
		// fetch languages if necessary 
		synchronized (subLanguageCache) {
			if (subLanguageCache.isEmpty()) {
				for (Entry<String, String> entry : xmlrpc.getSubLanguages().entrySet()) {
					// map id by name
					subLanguageCache.put(entry.getValue(), entry.getKey());
				}
				
				// some additional special handling
				subLanguageCache.put("Brazilian", "pob");
			}
		}
		
		String id = subLanguageCache.get(languageName);
		
		if (id == null) {
			throw new IllegalArgumentException(String.format("SubLanguageID for '%s' not found", languageName));
		}
		
		return id;
	}
	
}
