
package net.sourceforge.filebot.web;


import java.net.URL;
import java.util.Collections;

import net.sourceforge.tuned.DownloadTask;


public class SubsceneSubtitleDescriptor implements SubtitleDescriptor {
	
	private final String title;
	private final String language;
	private final String author;
	
	private final String typeId;
	
	private final URL downloadUrl;
	private final URL referer;
	
	
	public SubsceneSubtitleDescriptor(String title, String language, String author, String typeId, URL downloadUrl, URL referer) {
		this.title = title;
		this.language = language;
		this.author = author;
		
		this.typeId = typeId;
		
		this.downloadUrl = downloadUrl;
		this.referer = referer;
	}
	

	@Override
	public String getName() {
		return title;
	}
	

	public String getLanguageName() {
		return language;
	}
	

	public String getAuthor() {
		return author;
	}
	

	@Override
	public DownloadTask createDownloadTask() {
		DownloadTask downloadTask = new DownloadTask(downloadUrl);
		downloadTask.setRequestHeaders(Collections.singletonMap("Referer", referer.toString()));
		
		return downloadTask;
	}
	

	@Override
	public String getArchiveType() {
		return typeId;
	}
	

	@Override
	public String toString() {
		return String.format("%s [%s]", getName(), getLanguageName());
	}
	
}
