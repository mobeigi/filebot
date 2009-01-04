
package net.sourceforge.filebot.web;


import java.net.URI;
import java.util.Collection;

import javax.swing.Icon;


public interface EpisodeListClient {
	
	public Collection<SearchResult> search(String query) throws Exception;
	

	public boolean hasSingleSeasonSupport();
	

	public Collection<Episode> getEpisodeList(SearchResult searchResult) throws Exception;
	

	public Collection<Episode> getEpisodeList(SearchResult searchResult, int season) throws Exception;
	

	public URI getEpisodeListLink(SearchResult searchResult);
	

	public URI getEpisodeListLink(SearchResult searchResult, int season);
	

	public String getName();
	

	public Icon getIcon();
	
}
