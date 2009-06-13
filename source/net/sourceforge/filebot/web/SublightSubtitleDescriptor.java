
package net.sourceforge.filebot.web;


import java.nio.ByteBuffer;
import java.util.Formatter;
import java.util.concurrent.Callable;

import net.sublight.webservice.Subtitle;


public class SublightSubtitleDescriptor implements SubtitleDescriptor {
	
	private final Subtitle subtitle;
	private final SublightSubtitleClient source;
	
	private final String name;
	private final String languageName;
	

	public SublightSubtitleDescriptor(Subtitle subtitle, SublightSubtitleClient source) {
		this.subtitle = subtitle;
		this.source = source;
		
		this.name = getName(subtitle);
		this.languageName = source.getLanguageName(subtitle.getLanguage());
	}
	

	private String getName(Subtitle subtitle) {
		String releaseName = subtitle.getRelease();
		
		// check if release name contains sufficient information to be used as display name
		if (releaseName != null && !releaseName.isEmpty()) {
			boolean isValid = true;
			
			if (subtitle.getSeason() != null) {
				isValid &= releaseName.contains(subtitle.getSeason().toString());
			}
			
			if (subtitle.getEpisode() != null) {
				isValid &= releaseName.contains(subtitle.getEpisode().toString());
			}
			
			if (isValid) {
				return releaseName;
			}
		}
		
		// format proper display name
		Formatter builder = new Formatter(new StringBuilder(subtitle.getTitle()));
		
		if (subtitle.getSeason() != null || subtitle.getEpisode() != null) {
			builder.format(" - S%02dE%02d", subtitle.getSeason(), subtitle.getEpisode());
		}
		
		if (subtitle.getRelease() != null && !subtitle.getRelease().isEmpty()) {
			builder.format(" (%s)", subtitle.getRelease());
		}
		
		return builder.out().toString();
	}
	

	@Override
	public String getName() {
		return name;
	}
	

	@Override
	public String getLanguageName() {
		return languageName;
	}
	

	@Override
	public String getArchiveType() {
		return "zip";
	}
	

	@Override
	public Callable<ByteBuffer> getDownloadFunction() {
		return new Callable<ByteBuffer>() {
			
			@Override
			public ByteBuffer call() throws Exception {
				return ByteBuffer.wrap(source.getZipArchive(subtitle));
			}
		};
	}
	

	@Override
	public String toString() {
		return String.format("%s [%s]", getName(), getLanguageName());
	}
	
}
