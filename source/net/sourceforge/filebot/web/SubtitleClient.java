
package net.sourceforge.filebot.web;


import java.net.URI;
import java.util.Collection;
import java.util.Locale;

import javax.swing.Icon;


public interface SubtitleClient {
	
	public Collection<SearchResult> search(String searchterm) throws Exception;
	

	public Collection<SubtitleDescriptor> getSubtitleList(SearchResult searchResult, Locale language) throws Exception;
	

	public URI getSubtitleListLink(SearchResult searchResult);
	

	public String getName();
	

	public Icon getIcon();
	
}
