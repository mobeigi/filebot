
package net.sourceforge.filebot;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;


public class Settings {
	
	private static Settings settings = new Settings();
	
	
	public static Settings getSettings() {
		return settings;
	}
	
	private Preferences prefs;
	
	
	private Settings() {
		this.prefs = Preferences.userNodeForPackage(this.getClass());
	}
	
	private String defaultDelimiter = ";";
	
	
	private void putStringList(String key, Collection<String> values) {
		try {
			StringBuffer sb = new StringBuffer();
			
			for (String value : values) {
				sb.append(value.replaceAll(defaultDelimiter, " "));
				sb.append(defaultDelimiter);
			}
			
			prefs.put(key, sb.toString());
		} catch (IllegalArgumentException e) {
			// value might exceed max length
			Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, e.toString());
		}
	}
	

	private Collection<String> getStringList(String key) {
		String[] values = prefs.get(key, "").split(defaultDelimiter);
		
		List<String> list = new ArrayList<String>();
		
		for (String value : values) {
			if (!value.isEmpty())
				list.add(value);
		}
		
		return list;
	}
	
	
	private static enum Key {
		panel, tvshowcompletionterms, subtitlecompletionterms;
	}
	
	
	public int getSelectedPanel() {
		return prefs.getInt(Key.panel.name(), 3);
	}
	

	public void setSelectedPanel(int index) {
		prefs.putInt(Key.panel.name(), index);
	}
	

	public void setTvShowCompletionTerms(Collection<String> terms) {
		putStringList(Key.tvshowcompletionterms.name(), terms);
	}
	

	public Collection<String> getTvShowCompletionTerms() {
		return getStringList(Key.tvshowcompletionterms.name());
	}
	

	public void setSubtitleCompletionTerms(Collection<String> terms) {
		putStringList(Key.subtitlecompletionterms.name(), terms);
	}
	

	public Collection<String> getSubtitleCompletionTerms() {
		return getStringList(Key.subtitlecompletionterms.name());
	}
}
