
package net.sourceforge.filebot.web;


import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.ImageIcon;

import net.sourceforge.tuned.ProgressIterator;


public abstract class EpisodeListClient {
	
	private static List<EpisodeListClient> registry;
	
	
	public static synchronized List<EpisodeListClient> getAvailableEpisodeListClients() {
		if (registry == null) {
			registry = new ArrayList<EpisodeListClient>(3);
			
			registry.add(new TvdotcomClient());
			registry.add(new AnidbClient());
			registry.add(new TVRageClient());
		}
		
		return Collections.unmodifiableList(registry);
	}
	
	private final String name;
	private final boolean singleSeasonSupported;
	private final ImageIcon icon;
	
	
	public EpisodeListClient(String name, ImageIcon icon, boolean singleSeasonSupported) {
		this.name = name;
		this.icon = icon;
		this.singleSeasonSupported = singleSeasonSupported;
	}
	

	public abstract List<SearchResult> search(String searchterm) throws Exception;
	

	public abstract ProgressIterator<Episode> getEpisodeList(SearchResult searchResult, int season) throws Exception;
	

	public abstract URI getEpisodeListLink(SearchResult searchResult, int season);
	

	public boolean isSingleSeasonSupported() {
		return singleSeasonSupported;
	}
	

	public ImageIcon getIcon() {
		return icon;
	}
	

	public String getName() {
		return name;
	}
	

	@Override
	public String toString() {
		return name;
	}
	
}
