
package net.sourceforge.filebot.web;


import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.ImageIcon;

import net.sourceforge.tuned.ProgressIterator;


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
	
	private String name;
	private ImageIcon icon;
	
	
	public SubtitleClient(String name, ImageIcon icon) {
		this.name = name;
		this.icon = icon;
	}
	

	public abstract List<SearchResult> search(String searchterm) throws Exception;
	

	public abstract ProgressIterator<SubtitleDescriptor> getSubtitleList(SearchResult searchResult) throws Exception;
	

	public abstract URI getSubtitleListLink(SearchResult searchResult);
	

	public String getName() {
		return name;
	}
	

	public ImageIcon getIcon() {
		return icon;
	}
	

	@Override
	public String toString() {
		return name;
	}
}
