
package net.sourceforge.filebot;


import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import net.sourceforge.tuned.PreferencesList;
import net.sourceforge.tuned.PreferencesMap;


public class Settings {
	
	public static final String NAME = "FileBot";
	public static final String VERSION = "2.0";
	
	public static final String ROOT = NAME.toLowerCase();
	
	public static final String SELECTED_PANEL = "panel";
	public static final String SEARCH_HISTORY = "search/history";
	public static final String SUBTITLE_HISTORY = "subtitle/history";
	
	private static final Settings settings = new Settings();
	
	
	public static Settings getSettings() {
		return settings;
	}
	
	private final Preferences prefs;
	
	
	private Settings() {
		prefs = Preferences.userRoot().node(ROOT);
	}
	

	public void putInt(String key, int value) {
		prefs.putInt(key, value);
	}
	

	public int getInt(String key, int def) {
		return prefs.getInt(key, def);
	}
	

	public List<String> asStringList(String key) {
		return PreferencesList.map(prefs.node(key), String.class);
	}
	

	public Map<String, Boolean> asBooleanMap(String key) {
		return PreferencesMap.map(prefs.node(key), Boolean.class);
	}
	

	public void clear() {
		try {
			for (String child : prefs.childrenNames()) {
				prefs.node(child).removeNode();
			}
		} catch (BackingStoreException e) {
			Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, e.toString(), e);
		}
	}
	
}
