
package net.sourceforge.filebot.web;


import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.swing.Icon;


public abstract class SubtitleClient {
	
	private static List<SubtitleClient> registry;
	
	
	public static synchronized List<SubtitleClient> getAvailableSubtitleClients() {
		if (registry == null) {
			registry = new ArrayList<SubtitleClient>(2);
			
			registry.add(new OpenSubtitlesSubtitleClient());
			registry.add(new SubsceneSubtitleClient());
		}
		
		return Collections.unmodifiableList(registry);
	}
	
	private final String name;
	private final Icon icon;
	
	
	public SubtitleClient(String name, Icon icon) {
		this.name = name;
		this.icon = icon;
	}
	

	public abstract Collection<SearchResult> search(String searchterm) throws Exception;
	

	public abstract Collection<SubtitleDescriptor> getSubtitleList(SearchResult searchResult, Locale language) throws Exception;
	

	public abstract URI getSubtitleListLink(SearchResult searchResult);
	

	public String getName() {
		return name;
	}
	

	public Icon getIcon() {
		return icon;
	}
	

	@Override
	public String toString() {
		return name;
	}
}
