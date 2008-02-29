
package net.sourceforge.filebot;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;


public class Settings {
	
	private static Settings settings = new Settings();
	
	public static final String SELECTED_PANEL = "panel";
	public static final String SEARCH_HISTORY = "history/search";
	public static final String SUBTITLE_HISTORY = "history/subtitle";
	
	
	public static Settings getSettings() {
		return settings;
	}
	
	private Preferences prefs;
	
	
	private Settings() {
		this.prefs = Preferences.userRoot().node("filebot");
	}
	

	public void putString(String key, String value) {
		prefs.put(key, value);
	}
	

	public String getString(String key, String def) {
		return prefs.get(key, def);
	}
	

	public void putInt(String key, int value) {
		prefs.putInt(key, value);
	}
	

	public int getInt(String key, int def) {
		return prefs.getInt(key, def);
	}
	

	public void putBoolean(String key, boolean value) {
		prefs.putBoolean(key, value);
	}
	

	public boolean getBoolean(String key, boolean def) {
		return prefs.getBoolean(key, def);
	}
	

	public Collection<String> getStringList(String key) {
		Preferences listNode = prefs.node(key);
		
		List<String> list = new ArrayList<String>();
		
		try {
			for (String nodeKey : listNode.keys()) {
				list.add(listNode.get(nodeKey, null));
			}
		} catch (BackingStoreException e) {
			Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, e.toString());
		}
		
		return list;
	}
	

	public void putStringList(String key, Collection<String> list) {
		Preferences listNode = prefs.node(key);
		
		int i = 0;
		
		for (String entry : list) {
			listNode.put(Integer.toString(i), entry);
			i++;
		}
	}
	

	public Map<String, String> getStringMap(String key) {
		Preferences mapNode = prefs.node(key);
		
		Map<String, String> map = new HashMap<String, String>();
		
		try {
			for (String mapNodeKey : mapNode.keys()) {
				map.put(mapNodeKey, mapNode.get(mapNodeKey, null));
			}
		} catch (BackingStoreException e) {
			Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, e.toString());
		}
		
		return map;
	}
	

	public void putStringMap(String key, Map<String, String> map) {
		Preferences mapNode = prefs.node(key);
		
		for (Map.Entry<String, String> entry : map.entrySet()) {
			mapNode.put(entry.getKey(), entry.getValue());
		}
	}
}
