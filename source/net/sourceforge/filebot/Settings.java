
package net.sourceforge.filebot;


import java.util.List;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import net.sourceforge.tuned.ExceptionUtilities;
import net.sourceforge.tuned.PreferencesList;
import net.sourceforge.tuned.PreferencesMap;
import net.sourceforge.tuned.PreferencesMap.Adapter;


public final class Settings {
	
	public static String getApplicationName() {
		return "FileBot";
	};
	

	public static String getApplicationVersion() {
		return "1.9";
	};
	
	private static final Settings userRoot = new Settings(Preferences.userRoot(), getApplicationName());
	
	
	public static Settings userRoot() {
		return userRoot;
	}
	
	private final Preferences prefs;
	
	
	private Settings(Preferences parentNode, String name) {
		this.prefs = parentNode.node(name.toLowerCase());
	}
	

	public Settings node(String nodeName) {
		return new Settings(prefs, nodeName);
	}
	

	public void put(String key, String value) {
		prefs.put(key, value);
	}
	

	public void putDefault(String key, String value) {
		if (get(key) == null) {
			put(key, value);
		}
	}
	

	public String get(String key) {
		return get(key, null);
	}
	

	public String get(String key, String def) {
		return prefs.get(key, def);
	}
	

	public <T> Map<String, T> asMap(Class<T> type) {
		return PreferencesMap.map(prefs, type);
	}
	

	public <T> Map<String, T> asMap(Adapter<T> adapter) {
		return PreferencesMap.map(prefs, adapter);
	}
	

	public <T> List<T> asList(Class<T> type) {
		return PreferencesList.map(prefs, type);
	}
	

	public <T> List<T> asList(Adapter<T> adapter) {
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
