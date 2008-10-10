
package net.sourceforge.filebot.web;


import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Icon;

import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.Settings;


/**
 * {@link SubtitleClient} for OpenSubtitles.
 */
public class OpenSubtitlesSubtitleClient implements SubtitleClient {
	
	private final OpenSubtitlesClient client = new OpenSubtitlesClient(String.format("%s v%s", Settings.NAME, Settings.VERSION));
	
	private final LogoutTimer logoutTimer = new LogoutTimer();
	
	
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
	public List<SearchResult> search(String searchterm) throws Exception {
		login();
		
		List result = client.searchMoviesOnIMDB(searchterm);
		return result;
	}
	

	@SuppressWarnings("unchecked")
	@Override
	public Collection<SubtitleDescriptor> getSubtitleList(SearchResult searchResult, Locale language) throws Exception {
		login();
		
		int imdbId = ((MovieDescriptor) searchResult).getImdbId();
		
		return (Collection) client.searchSubtitles(imdbId, language);
	}
	

	@Override
	public URI getSubtitleListLink(SearchResult searchResult) {
		return null;
	}
	

	private synchronized void login() throws Exception {
		if (!client.isLoggedOn()) {
			client.loginAnonymous();
			Runtime.getRuntime().addShutdownHook(logoutShutdownHook);
		}
		
		logoutTimer.restart();
	}
	

	private synchronized void logout() {
		logoutTimer.stop();
		
		if (client.isLoggedOn()) {
			try {
				client.logout();
			} catch (Exception e) {
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, "Exception while deactivating session", e);
			} finally {
				Runtime.getRuntime().removeShutdownHook(logoutShutdownHook);
			}
		}
	}
	
	private final Thread logoutShutdownHook = new Thread() {
		
		@Override
		public void run() {
			logout();
		}
	};
	
	
	private class LogoutTimer {
		
		private static final long LOGOUT_DELAY = 12 * 60 * 1000; // 12 minutes
		
		private Timer daemon = null;
		private LogoutTimerTask currentTimerTask = null;
		
		
		public synchronized void restart() {
			if (daemon == null) {
				daemon = new Timer(getClass().getName(), true);
			}
			
			if (currentTimerTask != null) {
				currentTimerTask.cancel();
			}
			
			currentTimerTask = new LogoutTimerTask();
			daemon.schedule(currentTimerTask, LOGOUT_DELAY);
		}
		

		public synchronized void stop() {
			if (daemon == null)
				return;
			
			currentTimerTask.cancel();
			currentTimerTask = null;
			
			daemon.cancel();
			daemon = null;
		}
		
		
		private class LogoutTimerTask extends TimerTask {
			
			@Override
			public void run() {
				logout();
			};
		};
	}
	
}
