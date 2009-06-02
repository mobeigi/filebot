
package net.sourceforge.filebot.web;


import java.nio.ByteBuffer;

import javax.swing.SwingWorker;

import net.sublight.webservice.Subtitle;


public class SublightSubtitleDescriptor implements SubtitleDescriptor {
	
	private final Subtitle subtitle;
	private final SublightSubtitleClient source;
	

	public SublightSubtitleDescriptor(Subtitle subtitle, SublightSubtitleClient source) {
		this.subtitle = subtitle;
		this.source = source;
	}
	

	@Override
	public String getName() {
		// use release name by default
		String releaseName = subtitle.getRelease();
		
		if (releaseName == null || releaseName.isEmpty()) {
			// create name from subtitle information (name, season, episode, ...)
			String season = subtitle.getSeason() != null ? subtitle.getSeason().toString() : null;
			String episode = subtitle.getEpisode() != null ? subtitle.getEpisode().toString() : null;
			
			return EpisodeFormat.getInstance().format(new Episode(subtitle.getTitle(), season, episode, null));
		}
		
		return releaseName;
	}
	

	@Override
	public String getLanguageName() {
		return subtitle.getLanguage().value();
	}
	

	@Override
	public String getArchiveType() {
		return "zip";
	}
	

	@Override
	public SwingWorker<ByteBuffer, ?> createDownloadTask() {
		return new SwingWorker<ByteBuffer, Void>() {
			
			@Override
			protected ByteBuffer doInBackground() throws Exception {
				return ByteBuffer.wrap(source.getZipArchive(subtitle));
			}
		};
	}
	

	@Override
	public String toString() {
		return String.format("%s [%s]", getName(), getLanguageName());
	}
	
}
