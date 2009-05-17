
package net.sourceforge.filebot.web;


import static net.sourceforge.filebot.web.SubtitleSourceClient.*;

import java.net.MalformedURLException;
import java.net.URL;

import net.sourceforge.tuned.DownloadTask;


public class SubtitleSourceSubtitleDescriptor implements SubtitleDescriptor {
	
	private final int id;
	
	private final String releaseName;
	private final String language;
	
	private final String title;
	private final int season;
	private final int episode;
	
	
	public SubtitleSourceSubtitleDescriptor(int id, String releaseName, String language, String title, int season, int episode) {
		this.id = id;
		this.releaseName = releaseName;
		this.language = language;
		this.title = title;
		this.season = season;
		this.episode = episode;
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
	public DownloadTask createDownloadTask() {
		try {
			// e.g. http://www.subtitlesource.org/download/zip/760
			return new DownloadTask(new URL("http", HOST, "/download/zip/" + id));
		} catch (MalformedURLException e) {
			throw new UnsupportedOperationException(e);
		}
	}
	

	@Override
	public String getArchiveType() {
		return "zip";
	}
	

	@Override
	public String toString() {
		return String.format("%s [%s]", getName(), getLanguageName());
	}
	
}
