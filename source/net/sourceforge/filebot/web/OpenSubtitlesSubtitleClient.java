
package net.sourceforge.filebot.web;


import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.filebot.Settings;
import net.sourceforge.filebot.resources.ResourceManager;


/**
 * Client for the OpenSubtitles XML-RPC API.
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
		activate();
		
		return client.searchMoviesOnIMDB(query);
	}
	

	@Override
	public List<OpenSubtitlesSubtitleDescriptor> getSubtitleList(MovieDescriptor descriptor) throws Exception {
		activate();
		
		return client.searchSubtitles(descriptor.getImdbId());
	}
	

	private synchronized void activate() {
		try {
			if (!client.isLoggedOn()) {
				client.loginAnonymous();
			}
		} catch (Exception e) {
			Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, e.toString(), e);
		}
		
		logoutTimer.restart();
	}
	
	private final Runnable doLogout = new Runnable() {
		
		public void run() {
			logoutTimer.stop();
			
			if (client.isLoggedOn()) {
				client.logout();
			}
		}
	};
	
	
	private class LogoutTimer {
		
		private static final int LOGOUT_DELAY = 15000; // 12 minutes
		
		private Timer daemon = null;
		private LogoutTimerTask currentTimerTask = null;
		
		
		public synchronized void restart() {
			if (daemon == null) {
				daemon = new Timer(getClass().getName(), true);
			}
			
			if (currentTimerTask != null) {
				currentTimerTask.cancel();
				daemon.purge();
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
				doLogout.run();
			};
		};
	}
	
}
