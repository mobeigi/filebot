
package net.sourceforge.filebot.web;


import java.net.URI;
import java.util.List;
import java.util.Locale;

import javax.swing.Icon;


public interface EpisodeListProvider {
	
	public String getName();
	

	public Icon getIcon();
	

	public boolean hasSingleSeasonSupport();
	

	public boolean hasLocaleSupport();
	

	public List<SearchResult> search(String query) throws Exception;
	

	public List<SearchResult> search(String query, Locale locale) throws Exception;
	

	public List<Episode> getEpisodeList(SearchResult searchResult) throws Exception;
	

	public List<Episode> getEpisodeList(SearchResult searchResult, int season) throws Exception;
	

	public List<Episode> getEpisodeList(SearchResult searchResult, Locale locale) throws Exception;
	

	public List<Episode> getEpisodeList(SearchResult searchResult, int season, Locale locale) throws Exception;
	

	public URI getEpisodeListLink(SearchResult searchResult);
	

	public URI getEpisodeListLink(SearchResult searchResult, int season);
	
}
