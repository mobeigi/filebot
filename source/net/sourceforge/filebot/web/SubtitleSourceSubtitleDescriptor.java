
package net.sourceforge.filebot.web;


import java.net.URL;
import java.nio.ByteBuffer;


public class SubtitleSourceSubtitleDescriptor implements SubtitleDescriptor {
	
	private final String releaseName;
	private final String language;
	
	private final String title;
	private final int season;
	private final int episode;
	
	private final URL downloadLink;
	

	public SubtitleSourceSubtitleDescriptor(String releaseName, String language, String title, int season, int episode, URL downloadLink) {
		this.releaseName = releaseName;
		this.language = language;
		this.title = title;
		this.season = season;
		this.episode = episode;
		this.downloadLink = downloadLink;
	}
	

	@Override
	public String getName() {
		if (releaseName == null || releaseName.isEmpty()) {
			if (season == 0 && episode == 0) {
				return title;
			}
			
			StringBuilder sb = new StringBuilder(title).append(" - ");
			
			if (season != 0) {
				sb.append(season);
				
				if (episode != 0) {
					sb.append("x").append(episode);
				}
			} else {
				// episode cannot be 0 at this point
				sb.append(episode);
			}
			
			return sb.toString();
		}
		
		return releaseName;
	}
	

	@Override
	public String getLanguageName() {
		return language;
	}
	

	@Override
	public String getType() {
		return "zip";
	}
	

	@Override
	public ByteBuffer fetch() throws Exception {
		return WebRequest.fetch(downloadLink);
	}
	

	@Override
	public String toString() {
		return String.format("%s [%s]", getName(), getLanguageName());
	}
	
}
