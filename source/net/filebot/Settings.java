package net.filebot;

import static net.filebot.Logging.*;
import static net.filebot.util.FileUtilities.*;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import net.filebot.UserFiles.FileChooser;
import net.filebot.archive.Archive.Extractor;
import net.filebot.cli.ArgumentBean;
import net.filebot.util.ExceptionUtilities;
import net.filebot.util.PreferencesList;
import net.filebot.util.PreferencesMap;
import net.filebot.util.PreferencesMap.PreferencesEntry;
import net.filebot.util.PreferencesMap.StringAdapter;

public final class Settings {

	public static String getApplicationName() {
		return getApplicationProperty("application.name");
	}

	public static String getApplicationVersion() {
		return getApplicationProperty("application.version");
	}

	public static int getApplicationRevisionNumber() {
		try {
			return Integer.parseInt(getApplicationProperty("application.revision"));
		} catch (Exception e) {
			return 0;
		}
	}

	public static String getApplicationProperty(String key) {
		return ResourceBundle.getBundle(Settings.class.getName(), Locale.ROOT).getString(key);
	}

	public static String getApiKey(String name) {
		ResourceBundle bundle = ResourceBundle.getBundle(Settings.class.getName(), Locale.ROOT);
		if (isAppStore()) {
			try {
				return bundle.getString("apikey.appstore." + name);
			} catch (MissingResourceException e) {
				// use default value
			}
		}
		return bundle.getString("apikey." + name);
	}

	public static boolean isUnixFS() {
		return Boolean.parseBoolean(System.getProperty("unixfs"));
	}

	public static boolean useNativeShell() {
		return Boolean.parseBoolean(System.getProperty("useNativeShell"));
	}

	public static boolean useGVFS() {
		return Boolean.parseBoolean(System.getProperty("useGVFS"));
	}

	public static boolean useExtendedFileAttributes() {
		return Boolean.parseBoolean(System.getProperty("useExtendedFileAttributes"));
	}

	public static boolean useCreationDate() {
		return Boolean.parseBoolean(System.getProperty("useCreationDate"));
	}

	public static boolean useRenameHistory() {
		return Boolean.parseBoolean(System.getProperty("application.rename.history", "true"));
	}

	public static String getApplicationDeployment() {
		return System.getProperty("application.deployment", "jar");
	}

	public static boolean isAppStore() {
		return isApplicationDeployment("mas", "usc");
	}

	public static boolean isUbuntuApp() {
		return isApplicationDeployment("usc");
	}

	public static boolean isMacApp() {
		return isApplicationDeployment("mas", "app");
	}

	public static boolean isMacSandbox() {
		return isApplicationDeployment("mas");
	}

	public static boolean isInstalled() {
		return isApplicationDeployment("mas", "usc", "msi", "spk", "aur");
	}

	private static boolean isApplicationDeployment(String... ids) {
		String current = getApplicationDeployment();
		for (String id : ids) {
			if (current != null && current.equals(id))
				return true;
		}
		return false;
	}

	public static FileChooser getPreferredFileChooser() {
		return FileChooser.valueOf(System.getProperty("net.filebot.UserFiles.fileChooser", "Swing"));
	}

	public static Extractor getPreferredArchiveExtractor() {
		return Extractor.valueOf(System.getProperty("net.filebot.Archive.extractor", "SevenZipNativeBindings"));
	}

	public static int getPreferredThreadPoolSize() {
		try {
			String threadPool = System.getProperty("threadPool");
			if (threadPool != null) {
				return Integer.parseInt(threadPool);
			}
		} catch (Exception e) {
			debug.log(Level.WARNING, e.getMessage(), e);
		}

		return Runtime.getRuntime().availableProcessors();
	}

	public static File getApplicationFolder() {
		String applicationFolderPath = System.getProperty("application.dir");
		File applicationFolder = null;

		if (applicationFolderPath != null && !applicationFolderPath.isEmpty()) {
			// use given path
			applicationFolder = new File(applicationFolderPath);
		} else {
			// create folder in user home (can't use working directory for web start applications)
			applicationFolder = new File(System.getProperty("user.home"), ".filebot");
		}

		// create folder if necessary
		try {
			createFolders(applicationFolder);
		} catch (Exception e) {
			throw new IllegalStateException("application.dir", e);
		}

		return applicationFolder;
	}

	public static File getApplicationCache() {
		String cacheFolderPath = System.getProperty("application.cache");
		File cacheFolder = null;

		if (cacheFolderPath != null && !cacheFolderPath.isEmpty()) {
			cacheFolder = new File(cacheFolderPath);
		} else {
			cacheFolder = new File(getApplicationFolder(), "cache");
		}

		return cacheFolder;
	}

	public static File getApplicationTempFolder() {
		return new File(System.getProperty("java.io.tmpdir"));
	}

	public static File getRealUserHome() {
		if (isMacSandbox()) {
			// when running sandboxed applications user.home may point to the application-specific container
			String username = System.getProperty("user.name");
			if (username != null && username.length() > 0) {
				return new File("/Users", username);
			}
		}

		// default home
		return new File(System.getProperty("user.home"));
	}

	public static String getAppStoreName() {
		if (isMacApp())
			return "Mac App Store";
		if (isUbuntuApp())
			return "Ubuntu Software Center";

		return null;
	}

	public static String getAppStoreLink() {
		if (isMacApp())
			return getApplicationProperty("link.mas");
		if (isUbuntuApp())
			return getApplicationProperty("link.usc");

		return null;
	}

	public static String getDonateURL() {
		return getApplicationProperty("donate.url") + "?src=" + getApplicationDeployment();
	}

	public static String getEmbeddedHelpURL() {
		// add #hash so we can dynamically adjust the slides for the various platforms via JavaScript
		return getApplicationProperty("link.app.help") + '#' + getApplicationDeployment();
	}

	public static Map<String, String> getHelpURIs() {
		Map<String, String> links = new LinkedHashMap<String, String>();

		links.put("Getting Started", getApplicationProperty("link.intro"));
		links.put("FAQ", getApplicationProperty("link.faq"));
		links.put("Forums", getApplicationProperty("link.forums"));

		if (isMacSandbox()) {
			links.put("Report Bugs", getApplicationProperty("link.help.mas"));
			links.put("Request Help", getApplicationProperty("link.help.mas"));
		} else {
			links.put("Report Bugs", getApplicationProperty("link.bugs"));
			links.put("Request Help", getApplicationProperty("link.help"));
		}

		links.put("Contact us on Twitter", getApplicationProperty("link.twitter"));
		links.put("Contact us on Facebook", getApplicationProperty("link.facebook"));

		return links;
	}

	public static Settings forPackage(Class<?> type) {
		return new Settings(Preferences.userNodeForPackage(type));
	}

	private final Preferences prefs;

	private Settings(Preferences prefs) {
		this.prefs = prefs;
	}

	public Settings node(String nodeName) {
		return new Settings(prefs.node(nodeName));
	}

	public String get(String key) {
		return get(key, null);
	}

	public String get(String key, String def) {
		return prefs.get(key, def);
	}

	public void put(String key, String value) {
		if (value != null) {
			prefs.put(key, value);
		} else {
			remove(key);
		}
	}

	public void remove(String key) {
		prefs.remove(key);
	}

	public PreferencesEntry<String> entry(String key) {
		return new PreferencesEntry<String>(prefs, key, new StringAdapter());
	}

	public PreferencesMap<String> asMap() {
		return PreferencesMap.map(prefs);
	}

	public PreferencesList<String> asList() {
		return PreferencesList.map(prefs);
	}

	public void clear() {
		try {
			// remove child nodes
			for (String nodeName : prefs.childrenNames()) {
				prefs.node(nodeName).removeNode();
			}

			// remove entries
			prefs.clear();
		} catch (BackingStoreException e) {
			throw ExceptionUtilities.asRuntimeException(e);
		}
	}

	public static String getApplicationIdentifier() {
		return String.format("%s %s (r%d)", getApplicationName(), getApplicationVersion(), getApplicationRevisionNumber());
	}

	public static String getJavaRuntimeIdentifier() {
		return String.format("%s %s %s", System.getProperty("java.runtime.name"), System.getProperty("java.version"), GraphicsEnvironment.isHeadless() ? "(headless)" : "").trim();
	}

	private static String[] applicationArgumentArray;

	protected static void setApplicationArgumentArray(String[] args) {
		applicationArgumentArray = args;
	}

	public static ArgumentBean getApplicationArguments() {
		try {
			return ArgumentBean.parse(applicationArgumentArray);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

}
