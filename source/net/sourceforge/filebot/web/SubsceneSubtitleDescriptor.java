
package net.sourceforge.filebot.web;


import java.net.URL;
import java.util.Map;

import net.sourceforge.tuned.DownloadTask;


public class SubsceneSubtitleDescriptor implements SubtitleDescriptor {
	
	private final String title;
	private final String language;
	private final int numberOfCDs;
	private final String author;
	
	private final Map<String, String> downloadParameters;
	private final URL downloadUrl;
	
	
	public SubsceneSubtitleDescriptor(String title, String language, int numberOfCDs, String author, URL downloadUrl, Map<String, String> downloadParameters) {
		this.title = title;
		this.language = language;
		this.numberOfCDs = numberOfCDs;
		this.author = author;
		
		this.downloadUrl = downloadUrl;
		this.downloadParameters = downloadParameters;
	}
	

	public String getName() {
		return title;
	}
	

	public String getLanguageName() {
		return language;
	}
	

	public int getNumberOfCDs() {
		return numberOfCDs;
	}
	

	public String getAuthor() {
		return author;
	}
	

	public DownloadTask createDownloadTask() {
		return new DownloadTask(downloadUrl, downloadParameters);
	}
	

	public String getArchiveType() {
		return downloadParameters.get("typeId");
	}
	

	@Override
	public String toString() {
		return String.format("%s [%s]", title, language);
	}
	
}
