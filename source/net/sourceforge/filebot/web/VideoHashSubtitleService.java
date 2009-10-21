
package net.sourceforge.filebot.web;


import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;


public interface VideoHashSubtitleService {
	
	public Map<File, List<SubtitleDescriptor>> getSubtitleList(File[] videoFiles, String languageName) throws Exception;
	

	public boolean publishSubtitle(int imdbid, String languageName, File videoFile, File subtitleFile) throws Exception;
	

	public String getName();
	

	public URI getLink();
	

	public Icon getIcon();
	
}
