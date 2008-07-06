
package net.sourceforge.filebot.web;


import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.Icon;


public abstract class EpisodeListClient {
	
	private static List<EpisodeListClient> registry;
	
	
	public static synchronized List<EpisodeListClient> getAvailableEpisodeListClients() {
		if (registry == null) {
			registry = new ArrayList<EpisodeListClient>(3);
			
			registry.add(new TVDotComClient());
			registry.add(new AnidbClient());
			registry.add(new TVRageClient());
		}
		
		return Collections.unmodifiableList(registry);
	}
	
	private final String name;
	private final Icon icon;
	
	
	public EpisodeListClient(String name, Icon icon) {
		this.name = name;
		this.icon = icon;
	}
	

	public abstract Collection<SearchResult> search(String searchterm) throws Exception;
	

	public abstract boolean hasSingleSeasonSupport();
	

	public abstract Collection<Episode> getEpisodeList(SearchResult searchResult) throws Exception;
	

	public abstract Collection<Episode> getEpisodeList(SearchResult searchResult, int season) throws Exception;
	

	public abstract URI getEpisodeListLink(SearchResult searchResult);
	

	public abstract URI getEpisodeListLink(SearchResult searchResult, int season);
	

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
