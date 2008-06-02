
package net.sourceforge.filebot.web;


import java.net.URL;

import net.sourceforge.tuned.DownloadTask;


public class SubsceneSubtitleDescriptor implements SubtitleDescriptor {
	
	private final String title;
	private final String language;
	private final int numberOfCDs;
	private final String author;
	private final boolean hearingImpaired;
	
	private final String typeId;
	
	private final URL downloadUrl;
	private final URL referer;
	
	
	public SubsceneSubtitleDescriptor(String title, String language, int numberOfCDs, String author, boolean hearingImpaired, String typeId, URL downloadUrl, URL referer) {
		this.title = title;
		this.language = language;
		this.numberOfCDs = numberOfCDs;
		this.author = author;
		this.hearingImpaired = hearingImpaired;
		
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
		DownloadTask downloadTask = new DownloadTask(downloadUrl);
		downloadTask.setRequestHeader("Referer", referer.toString());
		
		return downloadTask;
	}
	

	@Override
	public String getArchiveType() {
		return typeId;
	}
	

	@Override
	public String toString() {
		return String.format("%s [%s]", title, language);
	}
	
}
