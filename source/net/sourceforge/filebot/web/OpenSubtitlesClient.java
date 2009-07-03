
package net.sourceforge.filebot.web;


import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Icon;

import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.tuned.Timer;


/**
 * SubtitleClient for OpenSubtitles.
 */
public class OpenSubtitlesClient implements SubtitleProvider {
	
	private final OpenSubtitlesXmlRpc xmlrpc;
	

	public OpenSubtitlesClient(String useragent) {
		this.xmlrpc = new OpenSubtitlesXmlRpc(useragent);
	}
	

	@Override
	public String getName() {
		return "OpenSubtitles";
	}
	

	@Override
	public Icon getIcon() {
		return ResourceManager.getIcon("search.opensubtitles");
	}
	

	@Override
	public List<SearchResult> search(String query) throws Exception {
		// require login
		login();
		
		@SuppressWarnings("unchecked")
		List<SearchResult> results = (List) xmlrpc.searchMoviesOnIMDB(query);
		
		return results;
	}
	

	@Override
	public List<SubtitleDescriptor> getSubtitleList(SearchResult searchResult, String languageName) throws Exception {
		// require login
		login();
		
		// singleton array with or empty array
		String[] languageFilter = languageName != null ? new String[] { getSubLanguageID(languageName) } : new String[0];
		
		@SuppressWarnings("unchecked")
		List<SubtitleDescriptor> subtitles = (List) xmlrpc.searchSubtitles(((MovieDescriptor) searchResult).getImdbId(), languageFilter);
		
		return subtitles;
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
			}
		}
		
		String id = subLanguageCache.get(languageName);
		
		if (id == null) {
			throw new IllegalArgumentException(String.format("SubLanguageID for '%s' not found", languageName));
		}
		
		return id;
	}
	
}
