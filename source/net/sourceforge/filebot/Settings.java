package net.sourceforge.filebot;

import static net.sourceforge.tuned.StringUtilities.*;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import net.sourceforge.tuned.ExceptionUtilities;
import net.sourceforge.tuned.PreferencesList;
import net.sourceforge.tuned.PreferencesMap;
import net.sourceforge.tuned.PreferencesMap.PreferencesEntry;
import net.sourceforge.tuned.PreferencesMap.StringAdapter;

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

	public static boolean useDonationReminder() {
		String deployment = getApplicationDeployment();
		for (String it : new String[] { "ppa", "appstore" }) {
			if (it.equalsIgnoreCase(deployment)) {
				return false;
			}
		}
		return true;
	}

	public static int getPreferredThreadPoolSize() {
		try {
			return Integer.parseInt(System.getProperty("threadPool"));
		} catch (Exception e) {
			return Runtime.getRuntime().availableProcessors();
		}
	}

	public static String getApplicationDeployment() {
		String deployment = System.getProperty("application.deployment");
		if (deployment != null)
			return deployment;

		if (System.getProperty("javawebstart.version") != null)
			return "webstart";

		return null;
	}

	public static File getApplicationFolder() {
		String applicationDirPath = System.getProperty("application.dir");
		File applicationFolder = null;

		if (applicationDirPath != null && applicationDirPath.length() > 0) {
			// use given path
			applicationFolder = new File(applicationDirPath);
		} else {
			// create folder in user home (can't use working directory for web start applications)
			applicationFolder = new File(System.getProperty("user.home"), ".filebot");
		}

		// create folder if necessary
		if (!applicationFolder.exists()) {
			applicationFolder.mkdirs();
		}

		return applicationFolder;
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
		return joinBy(" ", getApplicationName(), getApplicationVersion(), String.format("(r%s)", getApplicationRevisionNumber()));
	}

	public static String getJavaRuntimeIdentifier() {
		String name = System.getProperty("java.runtime.name");
		String version = System.getProperty("java.version");
		String headless = GraphicsEnvironment.isHeadless() ? "(headless)" : null;
		return joinBy(" ", name, version, headless);
	}

}
