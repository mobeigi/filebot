
package net.sourceforge.filebot;


import static com.dmurph.tracking.JGoogleAnalyticsTracker.GoogleAnalyticsVersion.*;
import static net.sourceforge.filebot.Settings.*;

import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.util.Map;
import java.util.logging.Logger;

import com.dmurph.tracking.AnalyticsConfigData;
import com.dmurph.tracking.JGoogleAnalyticsTracker;
import com.dmurph.tracking.VisitorData;
import com.sun.jna.Platform;


public class Analytics {
	
	private static final Map<String, String> persistentData = Settings.forPackage(Analytics.class).node("analytics").asMap();
	private static final String VISITOR_ID = "visitorId";
	private static final String TIMESTAMP_FIRST = "timestampFirst";
	private static final String TIMESTAMP_LAST = "timestampLast";
	private static final String VISITS = "visits";
	
	private static final VisitorData visitorData = restoreVisitorData();
	private static final JGoogleAnalyticsTracker tracker = new JGoogleAnalyticsTracker(getConfig(getApplicationProperty("analytics.WebPropertyID"), visitorData), V_4_7_2);
	

	public static void trackView(Class<?> view, String title) {
		trackView(view.getName().replace(',', '/'), title);
	}
	

	public static synchronized void trackView(String view, String title) {
		if (!tracker.isEnabled())
			return;
		
		tracker.trackPageViewFromSearch(view, title, "filebot.sourceforge.net", getJavaVersionIdentifier(), getDeploymentMethod());
	}
	

	public static void trackEvent(String category, String action, String label) {
		trackEvent(category, action, label, null);
	}
	

	public static synchronized void trackEvent(String category, String action, String label, Integer value) {
		if (!tracker.isEnabled())
			return;
		
		tracker.trackEvent(category, action, label, value);
	}
	

	public static void setEnabled(boolean enabled) {
		tracker.setEnabled(enabled);
	}
	

	private static String getDeploymentMethod() {
		return getApplicationDeployment() == null ? "fatjar" : getApplicationDeployment();
	}
	

	private static String getJavaVersionIdentifier() {
		return System.getProperty("java.runtime.name") + " " + System.getProperty("java.version");
	}
	

	private static AnalyticsConfigData getConfig(String webPropertyID, VisitorData visitorData) {
		AnalyticsConfigData config = new AnalyticsConfigData(webPropertyID, visitorData);
		
		config.setUserAgent(getUserAgent());
		config.setEncoding(System.getProperty("file.encoding"));
		config.setUserLanguage(getUserLanguage());
		
		try {
			GraphicsDevice[] display = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
			config.setScreenResolution(getScreenResolution(display));
			config.setColorDepth(getColorDepth(display));
		} catch (HeadlessException e) {
			Logger.getLogger(Analytics.class.getName()).warning(e.getMessage());
			config.setScreenResolution("80x25");
			config.setColorDepth("1");
		}
		
		return config;
	}
	

	private static String getUserAgent() {
		String wm = null;
		String os = null;
		
		if (Platform.isWindows()) {
			wm = "Windows";
			os = "Windows NT " + System.getProperty("os.version");
		} else if (Platform.isX11()) {
			wm = "X11";
			if (Platform.isLinux())
				os = "Linux " + System.getProperty("os.arch");
			else if (Platform.isSolaris())
				os = "SunOS " + System.getProperty("os.version");
			else if (Platform.isFreeBSD())
				os = "FreeBSD";
			else if (Platform.isOpenBSD())
				os = "OpenBSD";
		} else if (Platform.isMac()) {
			wm = "Macintosh";
			os = System.getProperty("os.name");
		}
		
		return String.format("%s/%s (%s; U; %s; JRE %s)", getApplicationName(), getApplicationVersion(), wm, os, System.getProperty("java.version"));
	}
	

	private static String getUserLanguage() {
		String region = System.getProperty("user.region");
		if (region == null)
			region = System.getProperty("user.country");
		
		return System.getProperty("user.language") + "-" + region;
	}
	

	private static String getScreenResolution(GraphicsDevice[] display) {
		int screenHeight = 0;
		int screenWidth = 0;
		
		// get size of each screen
		for (int i = 0; i < display.length; i++) {
			DisplayMode dm = display[i].getDisplayMode();
			screenWidth += dm.getWidth();
			screenHeight += dm.getHeight();
		}
		
		if (screenHeight <= 0 && screenWidth <= 0)
			throw new HeadlessException("Illegal screen size");
		
		return screenWidth + "x" + screenHeight;
	}
	

	private static String getColorDepth(GraphicsDevice[] display) {
		if (display[0] == null)
			return null;
		
		String colorDepth = display[0].getDisplayMode().getBitDepth() + "";
		for (int i = 1; i < display.length; i++) {
			colorDepth += ", " + display[i].getDisplayMode().getBitDepth();
		}
		
		return colorDepth;
	}
	

	private static VisitorData restoreVisitorData() {
		try {
			// try to restore visitor
			int visitorId = Integer.parseInt(persistentData.get(VISITOR_ID));
			long timestampFirst = Long.parseLong(persistentData.get(TIMESTAMP_FIRST));
			long timestampLast = Long.parseLong(persistentData.get(TIMESTAMP_LAST));
			int visits = Integer.parseInt(persistentData.get(VISITS));
			
			return VisitorData.newSession(visitorId, timestampFirst, timestampLast, visits);
		} catch (Exception e) {
			// new visitor
			return VisitorData.newVisitor();
		}
	}
	

	private static void storeVisitorData(VisitorData visitor) {
		persistentData.put(VISITOR_ID, String.valueOf(visitor.getVisitorId()));
		persistentData.put(TIMESTAMP_FIRST, String.valueOf(visitor.getTimestampFirst()));
		persistentData.put(TIMESTAMP_LAST, String.valueOf(visitor.getTimestampPrevious()));
		persistentData.put(VISITS, String.valueOf(visitor.getVisits()));
	}
	

	public static void completeTracking(long timeout) {
		storeVisitorData(visitorData);
		JGoogleAnalyticsTracker.completeBackgroundTasks(timeout);
	}
	

	static {
		Runtime.getRuntime().addShutdownHook(new Thread("AnalyticsShutdownHook") {
			
			@Override
			public void run() {
				completeTracking(2000);
			}
		});
	}
	

	/**
	 * Dummy constructor to prevent instantiation.
	 */
	private Analytics() {
		throw new UnsupportedOperationException();
	}
	
}
