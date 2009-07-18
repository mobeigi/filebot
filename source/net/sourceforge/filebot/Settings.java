
package net.sourceforge.filebot;


import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import net.sourceforge.tuned.ExceptionUtilities;
import net.sourceforge.tuned.PreferencesList;
import net.sourceforge.tuned.PreferencesMap;
import net.sourceforge.tuned.PreferencesMap.Adapter;
import net.sourceforge.tuned.PreferencesMap.PreferencesEntry;
import net.sourceforge.tuned.PreferencesMap.StringAdapter;


public final class Settings {
	
	public static String getApplicationName() {
		return "FileBot";
	};
	

	public static String getApplicationVersion() {
		return "1.9";
	};
	

	private static final Settings userRoot = new Settings(Preferences.userNodeForPackage(Settings.class));
	

	public static Settings userRoot() {
		return userRoot;
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
	

	public void putDefault(String key, String value) {
		if (get(key) == null) {
			put(key, value);
		}
	}
	

	public void remove(String key) {
		prefs.remove(key);
	}
	

	public PreferencesEntry<String> entry(String key) {
		return new PreferencesEntry<String>(prefs, key, new StringAdapter());
	}
	

	public <T> PreferencesEntry<T> entry(String key, Adapter<T> adapter) {
		return new PreferencesEntry<T>(prefs, key, adapter);
	}
	

	public PreferencesMap<String> asMap() {
		return PreferencesMap.map(prefs);
	}
	

	public <T> PreferencesMap<T> asMap(Adapter<T> adapter) {
		return PreferencesMap.map(prefs, adapter);
	}
	

	public PreferencesList<String> asList() {
		return PreferencesList.map(prefs);
	}
	

	public <T> PreferencesList<T> asList(Adapter<T> adapter) {
		return PreferencesList.map(prefs, adapter);
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
