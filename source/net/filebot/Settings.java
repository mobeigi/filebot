package net.filebot;

import static net.filebot.Logging.*;
import static net.filebot.util.FileUtilities.*;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import net.filebot.UserFiles.FileChooser;
import net.filebot.archive.Archive.Extractor;
import net.filebot.cli.ArgumentBean;
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

	public static File getApplicationFolder() {
		return ApplicationFolder.AppData.get(); // added for script compatibility
	}

	public static enum ApplicationFolder {

		AppData {

			@Override
			public File get() {
				String appdata = System.getProperty("application.dir");

				if (appdata != null) {
					// use given $APP_DATA folder
					return new File(appdata);
				} else {
					// use $HOME/.filebot as application data folder
					return new File(System.getProperty("user.home"), ".filebot");
				}
			}
		},

		UserHome {

			@Override
			public File get() {
				// The user.home of sandboxed applications will point to the application-specific container
				if (isMacSandbox()) {
					return new File("/Users", System.getProperty("user.name", "anonymous"));
				}

				// default user home
				return new File(System.getProperty("user.home"));
			}

		},

		Temp {

			@Override
			public File get() {
				return new File(System.getProperty("java.io.tmpdir"));
			}
		},

		Cache {

			@Override
			public File get() {
				String cache = System.getProperty("application.cache");
				if (cache != null) {
					return new File(cache);
				}

				// default to $APP_DATA/cache
				return AppData.resolve("cache");
			}
		};

		public abstract File get();

		public File resolve(String name) {
			return new File(getCanonicalFile(), name);
		}

		public File getCanonicalFile() {
			File path = get();
			try {
				return createFolders(path.getCanonicalFile());
			} catch (Exception e) {
				debug.log(Level.SEVERE, String.format("Failed to create application folder: %s => %s", this, path), e);
				return path;
			}
		}

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
			debug.warning(e.getMessage());
		}
	}

}
