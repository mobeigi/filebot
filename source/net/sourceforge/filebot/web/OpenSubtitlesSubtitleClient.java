
package net.sourceforge.filebot.web;


import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Icon;

import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.tuned.Timer;


/**
 * SubtitleClient for OpenSubtitles.
 */
public class OpenSubtitlesSubtitleClient implements SubtitleProvider {
	
	private final OpenSubtitlesClient client;
	

	public OpenSubtitlesSubtitleClient(String clientInfo) {
		this.client = new OpenSubtitlesClient(clientInfo);
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
		List<SearchResult> results = (List) client.searchMoviesOnIMDB(query);
		
		return results;
	}
	

	@Override
	public List<SubtitleDescriptor> getSubtitleList(SearchResult searchResult, String languageName) throws Exception {
		// require login
		login();
		
		@SuppressWarnings("unchecked")
		List<SubtitleDescriptor> subtitles = (List) client.searchSubtitles(((MovieDescriptor) searchResult).getImdbId(), languageName);
		
		return subtitles;
	}
	

	@Override
	public URI getSubtitleListLink(SearchResult searchResult, String languageName) {
		//TODO provide link
		return null;
	}
	

	protected synchronized void login() throws Exception {
		if (!client.isLoggedOn()) {
			client.loginAnonymous();
		}
		
		logoutTimer.set(10, TimeUnit.MINUTES, true);
	}
	

	protected synchronized void logout() {
		if (client.isLoggedOn()) {
			try {
				client.logout();
			} catch (Exception e) {
				Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Logout failed", e);
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
}
