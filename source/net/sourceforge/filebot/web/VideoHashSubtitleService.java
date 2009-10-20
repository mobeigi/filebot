
package net.sourceforge.filebot.web;


import java.io.File;
import java.util.List;
import java.util.Map;


public interface VideoHashSubtitleService {
	
	public Map<File, List<SubtitleDescriptor>> getSubtitleList(File[] videoFiles, String languageName) throws Exception;
	

	public boolean publishSubtitle(int imdbid, String languageName, File videoFile, File subtitleFile) throws Exception;
	
}
