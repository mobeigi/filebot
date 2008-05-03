
package net.sourceforge.filebot.web;


import java.net.URL;
import java.util.Map;

import net.sourceforge.tuned.DownloadTask;


public class SubsceneSubtitleDescriptor implements SubtitleDescriptor {
	
	private final String title;
	private final String language;
	private final int numberOfCDs;
	private final String author;
	private final boolean hearingImpaired;
	
	private final Map<String, String> downloadParameters;
	private final URL downloadUrl;
	
	
	public SubsceneSubtitleDescriptor(String title, String language, int numberOfCDs, String author, boolean hearingImpaired, URL downloadUrl, Map<String, String> downloadParameters) {
		this.title = title;
		this.language = language;
		this.numberOfCDs = numberOfCDs;
		this.author = author;
		this.hearingImpaired = hearingImpaired;
		
		this.downloadUrl = downloadUrl;
		this.downloadParameters = downloadParameters;
	}
	

	@Override
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
	

	public boolean getHearingImpaired() {
		return hearingImpaired;
	}
	

	@Override
	public DownloadTask createDownloadTask() {
		return new DownloadTask(downloadUrl, downloadParameters);
	}
	

	@Override
	public String getArchiveType() {
		return downloadParameters.get("typeId");
	}
	

	@Override
	public String toString() {
		return String.format("%s [%s]", title, language);
	}
	
}
