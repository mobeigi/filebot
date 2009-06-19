
package net.sourceforge.filebot.web;


import static java.util.Collections.*;

import java.net.URL;
import java.nio.ByteBuffer;


public class SubsceneSubtitleDescriptor implements SubtitleDescriptor {
	
	private final String title;
	private final String language;
	
	private final String archiveType;
	
	private final URL downloadLink;
	private final URL referer;
	

	public SubsceneSubtitleDescriptor(String title, String language, String archiveType, URL downloadLink, URL referer) {
		this.title = title;
		this.language = language;
		
		this.archiveType = archiveType;
		
		this.downloadLink = downloadLink;
		this.referer = referer;
	}
	

	@Override
	public String getName() {
		return title;
	}
	

	public String getLanguageName() {
		return language;
	}
	

	@Override
	public ByteBuffer fetch() throws Exception {
		return WebRequest.fetch(downloadLink, singletonMap("Referer", referer.toString()));
	}
	

	@Override
	public String getType() {
		return archiveType;
	}
	

	@Override
	public String toString() {
		return String.format("%s [%s]", getName(), getLanguageName());
	}
	
}
