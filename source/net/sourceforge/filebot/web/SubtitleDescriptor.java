
package net.sourceforge.filebot.web;


import net.sourceforge.tuned.DownloadTask;


public interface SubtitleDescriptor {
	
	public String getName();
	

	public String getLanguageName();
	

	public String getAuthor();
	

	public String getArchiveType();
	

	public DownloadTask createDownloadTask();
	
}
