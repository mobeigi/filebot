
package net.sourceforge.filebot;


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
	};
	

	public static String getApplicationVersion() {
		return getApplicationProperty("application.version");
	};
	

	public static String getApplicationProperty(String key) {
		return ResourceBundle.getBundle(Settings.class.getName(), Locale.ROOT).getString(key);
	}
	

	public static File getApplicationFolder() {
		// special handling for web start
		if (System.getProperty("application.deployment") != null || System.getProperty("javawebstart.version") != null) {
			// can't use working directory for web start applications
			File folder = new File(System.getProperty("user.home"), ".filebot");
			
			// create folder if necessary 
			if (!folder.exists()) {
				folder.mkdir();
			}
			
			return folder;
		}
		
		// use working directory
		return new File(System.getProperty("user.dir"));
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
		prefs.put(key, value);
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
}
