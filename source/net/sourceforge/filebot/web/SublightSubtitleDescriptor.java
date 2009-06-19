
package net.sourceforge.filebot.web;


import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.Formatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import net.sourceforge.tuned.ByteBufferOutputStream;
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
	public String getType() {
		return subtitle.getSubtitleType().value().toLowerCase();
	}
	

	@Override
	public ByteBuffer fetch() throws Exception {
		byte[] archive = source.getZipArchive(subtitle);
		
		// the zip archive will contain exactly one subtitle
		ZipInputStream stream = new ZipInputStream(new ByteArrayInputStream(archive));
		
		try {
			// move to subtitle entry
			ZipEntry entry = stream.getNextEntry();
			
			ByteBufferOutputStream buffer = new ByteBufferOutputStream((int) entry.getSize());
			
			// read subtitle data
			buffer.transferFully(stream);
			
			// return plain subtitle data
			return buffer.getByteBuffer();
		} finally {
			stream.close();
		}
	}
	

	@Override
	public String toString() {
		return String.format("%s [%s]", getName(), getLanguageName());
	}
	
}
