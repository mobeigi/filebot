
package net.sourceforge.filebot.web;


import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.filebot.Settings;
import net.sourceforge.filebot.resources.ResourceManager;


/**
 * {@link SubtitleClient} for OpenSubtitles.
 * 
 */
public class OpenSubtitlesSubtitleClient extends SubtitleClient {
	
	private final OpenSubtitlesClient client = new OpenSubtitlesClient(String.format("%s v%s", Settings.NAME, Settings.VERSION));
	
	private final LogoutTimer logoutTimer = new LogoutTimer();
	
	
	public OpenSubtitlesSubtitleClient() {
		super("OpenSubtitles", ResourceManager.getIcon("search.opensubtitles"));
		
		Runtime.getRuntime().addShutdownHook(new Thread(doLogout));
	}
	

	@Override
	public List<MovieDescriptor> search(String query) throws Exception {
		login();
		
		return client.searchMoviesOnIMDB(query);
	}
	

	@Override
	public List<OpenSubtitlesSubtitleDescriptor> getSubtitleList(MovieDescriptor descriptor) throws Exception {
		login();
		
		return client.searchSubtitles(descriptor.getImdbId());
	}
	

	private synchronized void login() throws Exception {
		if (!client.isLoggedOn()) {
			client.loginAnonymous();
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
			}
		}
	}
	
	private final Runnable doLogout = new Runnable() {
		
		@Override
		public void run() {
			logout();
		}
	};
	
	
	private class LogoutTimer {
		
		private final long LOGOUT_DELAY = 12 * 60 * 1000; // 12 minutes
		
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
