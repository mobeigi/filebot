package net.sourceforge.filebot;

import static com.dmurph.tracking.JGoogleAnalyticsTracker.GoogleAnalyticsVersion.*;
import static net.sourceforge.filebot.Settings.*;

import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.dmurph.tracking.AnalyticsConfigData;
import com.dmurph.tracking.JGoogleAnalyticsTracker;
import com.dmurph.tracking.VisitorData;
import com.sun.jna.Platform;

public class Analytics {

	private static JGoogleAnalyticsTracker tracker;
	private static VisitorData visitorData;

	private static String host = "filebot.sourceforge.net";
	private static String currentView = null;

	private static boolean enabled = false;

	public static synchronized JGoogleAnalyticsTracker getTracker() throws Throwable {
		if (tracker != null)
			return tracker;

		// disable useless background logging, if it doesn't work it doesn't work, won't affect anything (putting it here works for Java 6)
		Logger.getLogger("com.dmurph.tracking").setLevel(Level.OFF);
		Logger.getLogger("java.util.prefs").setLevel(Level.OFF);

		// initialize tracker
		visitorData = restoreVisitorData();
		tracker = new JGoogleAnalyticsTracker(getConfig(getApplicationProperty("analytics.WebPropertyID"), visitorData), V_4_7_2);

		// store session data on shutdown
		Runtime.getRuntime().addShutdownHook(new Thread("AnalyticsShutdownHook") {

			@Override
			public void run() {
				storeVisitorData(visitorData);
				JGoogleAnalyticsTracker.completeBackgroundTasks(2000);
			}
		});

		return tracker;
	}

	public static void trackView(Class<?> view, String title) {
		trackView(view.getName().replace('.', '/'), title);
	}

	public static synchronized void trackView(String view, String title) {
		if (!enabled)
			return;

		if (currentView == null) {
			// track application startup
			try {
				getTracker().trackPageViewFromSearch(view, title, host, getJavaRuntimeIdentifier(), getDeploymentMethod());
			} catch (Throwable e) {
				Logger.getLogger(Analytics.class.getName()).log(Level.WARNING, e.toString());
			}
		} else {
			// track application state change
			try {
				getTracker().trackPageViewFromReferrer(view, title, host, host, currentView);
			} catch (Throwable e) {
				Logger.getLogger(Analytics.class.getName()).log(Level.WARNING, e.toString());
			}
		}

		currentView = view;
	}

	public static void trackEvent(String category, String action, String label) {
		trackEvent(category, action, label, null);
	}

	public static synchronized void trackEvent(String category, String action, String label, Integer value) {
		if (!enabled)
			return;

		try {
			getTracker().trackEvent(normalize(category), normalize(action), normalize(label), value);
		} catch (Throwable e) {
			Logger.getLogger(Analytics.class.getName()).log(Level.WARNING, e.toString());
		}
	}

	private static String normalize(String label) {
		if (label == null)
			return null;

		// trim braces
		return label.replaceAll("[*()]", "").trim();
	}

	public static synchronized void setEnabled(boolean b) {
		enabled = b;
	}

	public static boolean isEnabled() {
		return enabled;
	}

	private static String getDeploymentMethod() {
		return getApplicationDeployment() == null ? "fatjar" : getApplicationDeployment();
	}

	private static AnalyticsConfigData getConfig(String webPropertyID, VisitorData visitorData) {
		AnalyticsConfigData config = new AnalyticsConfigData(webPropertyID, visitorData);

		config.setUserAgent(getUserAgent());
		config.setEncoding(System.getProperty("file.encoding"));
		config.setUserLanguage(getUserLanguage());

		try {
			if (GraphicsEnvironment.isHeadless())
				throw new HeadlessException();

			// desktop environment
			GraphicsDevice[] display = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
			config.setScreenResolution(getScreenResolution(display));
			config.setColorDepth(getColorDepth(display));
		} catch (Throwable e) {
			// headless environment
			config.setScreenResolution("80x25");
			config.setColorDepth("1");
		}

		return config;
	}

	private static String getUserAgent() {
		// initialize with default values
		String wm = System.getProperty("os.name");
		String os = System.getProperty("os.name") + " " + System.getProperty("os.version");

		try {
			if (Platform.isWindows()) {
				wm = "Windows";
				os = "Windows NT " + System.getProperty("os.version");
			} else if (Platform.isMac()) {
				wm = "Macintosh";
				os = System.getProperty("os.name");
			} else {
				if (!GraphicsEnvironment.isHeadless() && Platform.isX11())
					wm = "X11";

				if (Platform.isLinux())
					os = "Linux " + System.getProperty("os.arch");
				else if (Platform.isSolaris())
					os = "SunOS " + System.getProperty("os.version");
				else if (Platform.isFreeBSD())
					os = "FreeBSD";
				else if (Platform.isOpenBSD())
					os = "OpenBSD";
			}
		} catch (Throwable e) {
			// ignore any Platform detection issues and especially ignore LinkageErrors that might occur on headless machines
		}

		return String.format("%s/%s (%s; U; %s; JRE %s)", getApplicationName(), getApplicationVersion(), wm, os, System.getProperty("java.version"));
	}

	private static String getUserLanguage() {
		String language = System.getProperty("user.language");

		// user region or country
		String region = System.getProperty("user.region");
		if (region == null)
			region = System.getProperty("user.country");

		// return language string user language with or without user region
		if (region == null)
			return language;

		return language + "-" + region;
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

		return screenWidth + "x" + screenHeight;
	}

	private static String getColorDepth(GraphicsDevice[] display) {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < display.length; i++) {
			if (sb.length() > 0) {
				sb.append(", ");
			}

			sb.append(display[i].getDisplayMode().getBitDepth());
		}

		return sb.toString();
	}

	private static final String VISITOR_ID = "visitorId";
	private static final String TIMESTAMP_FIRST = "timestampFirst";
	private static final String TIMESTAMP_LAST = "timestampLast";
	private static final String VISITS = "visits";

	private synchronized static VisitorData restoreVisitorData() {
		try {
			// try to restore visitor
			Map<String, String> data = getPersistentData();
			int visitorId = Integer.parseInt(data.get(VISITOR_ID));
			long timestampFirst = Long.parseLong(data.get(TIMESTAMP_FIRST));
			long timestampLast = Long.parseLong(data.get(TIMESTAMP_LAST));
			int visits = Integer.parseInt(data.get(VISITS));

			return VisitorData.newSession(visitorId, timestampFirst, timestampLast, visits);
		} catch (Exception e) {
			// new visitor
			return VisitorData.newVisitor();
		}
	}

	private synchronized static void storeVisitorData(VisitorData visitor) {
		Map<String, String> data = getPersistentData();
		data.put(VISITOR_ID, String.valueOf(visitor.getVisitorId()));
		data.put(TIMESTAMP_FIRST, String.valueOf(visitor.getTimestampFirst()));
		data.put(TIMESTAMP_LAST, String.valueOf(visitor.getTimestampPrevious()));
		data.put(VISITS, String.valueOf(visitor.getVisits()));
	}

	private static Map<String, String> getPersistentData() {
		return Settings.forPackage(Analytics.class).node("analytics").asMap();
	}

	/**
	 * Dummy constructor to prevent instantiation
	 */
	private Analytics() {
		throw new UnsupportedOperationException();
	}

	static {
		// disable useless background logging, if it doesn't work it doesn't work, won't affect anything (putting it here works for Java 7)
		Logger.getLogger("com.dmurph.tracking").setLevel(Level.OFF);
	}

}
