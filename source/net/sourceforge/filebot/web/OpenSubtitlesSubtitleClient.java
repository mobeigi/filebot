
package net.sourceforge.filebot.web;


import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Icon;

import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.tuned.Timer;


/**
 * SubtitleClient for OpenSubtitles.
 */
public class OpenSubtitlesSubtitleClient implements SubtitleClient {
	
	private final OpenSubtitlesClient client;
	
	
	public OpenSubtitlesSubtitleClient(String useragent) {
		this.client = new OpenSubtitlesClient(useragent);
	}
	

	@Override
	public String getName() {
		return "OpenSubtitles";
	}
	

	@Override
	public Icon getIcon() {
		return ResourceManager.getIcon("search.opensubtitles");
	}
	

	@SuppressWarnings("unchecked")
	@Override
	public List<SearchResult> search(String query) throws Exception {
		login();
		
		return (List) client.searchMoviesOnIMDB(query);
	}
	

	@SuppressWarnings("unchecked")
	@Override
	public List<SubtitleDescriptor> getSubtitleList(SearchResult searchResult, Locale language) throws Exception {
		login();
		
		return (List) client.searchSubtitles(((MovieDescriptor) searchResult).getImdbId(), language);
	}
	

	@Override
	public URI getSubtitleListLink(SearchResult searchResult, Locale language) {
		//TODO provide link
		return null;
	}
	

	private synchronized void login() throws Exception {
		if (!client.isLoggedOn()) {
			client.loginAnonymous();
		}
		
		logoutTimer.set(12, TimeUnit.MINUTES, true);
	}
	

	private synchronized void logout() {
		logoutTimer.cancel();
		
		if (client.isLoggedOn()) {
			try {
				client.logout();
			} catch (Exception e) {
				Logger.getLogger("global").log(Level.SEVERE, "Exception while deactivating session", e);
			}
		}
	}
	
	private final Timer logoutTimer = new Timer() {
		
		@Override
		public void run() {
			logout();
		}
		
	};
}
