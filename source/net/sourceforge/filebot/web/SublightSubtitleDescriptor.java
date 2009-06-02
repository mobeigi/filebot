
package net.sourceforge.filebot.web;


import net.sourceforge.tuned.DownloadTask;
import net.sublight.webservice.Subtitle;


public class SublightSubtitleDescriptor implements SubtitleDescriptor {
	
	private final Subtitle subtitle;
	

	public SublightSubtitleDescriptor(Subtitle subtitle) {
		this.subtitle = subtitle;
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
	public String getArchiveType() {
		return subtitle.getSubtitleType().value().toLowerCase();
	}
	

	@Override
	public String getLanguageName() {
		return subtitle.getLanguage().value();
	}
	

	@Override
	public DownloadTask createDownloadTask() {
		// TODO support
		return new DownloadTask(null);
	}
	

	@Override
	public String toString() {
		return String.format("%s [%s]", getName(), getLanguageName());
	}
	
}
