
package net.sourceforge.filebot.web;


import java.net.URI;
import java.util.List;

import javax.swing.Icon;


public interface EpisodeListProvider {
	
	public List<SearchResult> search(String query) throws Exception;
	

	public boolean hasSingleSeasonSupport();
	

	public List<Episode> getEpisodeList(SearchResult searchResult) throws Exception;
	

	public List<Episode> getEpisodeList(SearchResult searchResult, int season) throws Exception;
	

	public URI getEpisodeListLink(SearchResult searchResult);
	

	public URI getEpisodeListLink(SearchResult searchResult, int season);
	

	public String getName();
	

	public Icon getIcon();
	
}
