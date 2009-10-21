
package net.sourceforge.filebot.web;


import java.net.URI;
import java.util.List;

import javax.swing.Icon;


public interface SubtitleProvider {
	
	public List<SearchResult> search(String query) throws Exception;
	

	public List<SubtitleDescriptor> getSubtitleList(SearchResult searchResult, String languageName) throws Exception;
	

	public URI getSubtitleListLink(SearchResult searchResult, String languageName);
	

	public String getName();
	

	public URI getLink();
	

	public Icon getIcon();
	
}
