
package net.sourceforge.filebot.web;


import java.net.URI;
import java.util.List;
import java.util.Locale;

import javax.swing.Icon;


public interface SubtitleProvider {
	
	public List<SearchResult> search(String query) throws Exception;
	

	public List<SubtitleDescriptor> getSubtitleList(SearchResult searchResult, Locale language) throws Exception;
	

	public URI getSubtitleListLink(SearchResult searchResult, Locale language);
	

	public String getName();
	

	public Icon getIcon();
	
}
