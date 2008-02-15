
package net.sourceforge.filebot.web;


import java.net.URL;
import java.util.Map;


public class SubsceneSubtitleDescriptor {
	
	private String title;
	private String language;
	private int numberOfCDs;
	private String author;
	
	private Map<String, String> downloadParameters;
	private URL downloadUrl;
	
	
	public SubsceneSubtitleDescriptor(String title, String language, int numberOfCDs, String author, URL downloadUrl, Map<String, String> downloadParameters) {
		this.title = title;
		this.language = language;
		this.numberOfCDs = numberOfCDs;
		this.author = author;
		
		this.downloadUrl = downloadUrl;
		this.downloadParameters = downloadParameters;
	}
	

	public String getTitle() {
		return title;
	}
	

	public String getLanguage() {
		return language;
	}
	

	public int getNumberOfCDs() {
		return numberOfCDs;
	}
	

	public String getAuthor() {
		return author;
	}
	

	public String getArchiveType() {
		return downloadParameters.get("typeId");
	}
	

	public DownloadTask createDownloadTask() {
		return new DownloadTask(downloadUrl, downloadParameters);
	}
	

	@Override
	public String toString() {
		return String.format("%s [%s]", title, language);
	}
	
}
