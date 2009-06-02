
package net.sourceforge.filebot.web;


import java.io.Serializable;


public class Episode implements Serializable {
	
	private final String seriesName;
	private final String season;
	private final String episode;
	private final String title;
	

	public Episode(String seriesName, int season, int episode, String title) {
		this(seriesName, String.valueOf(season), String.valueOf(episode), title);
	}
	

	public Episode(String seriesName, String season, String episode, String title) {
		this.seriesName = seriesName;
		this.season = season;
		this.episode = episode;
		this.title = title;
	}
	

	public String getEpisode() {
		return episode;
	}
	

	public Integer getEpisodeNumber() {
		try {
			return Integer.valueOf(episode);
		} catch (NumberFormatException e) {
			return null;
		}
	}
	

	public String getSeason() {
		return season;
	}
	

	public Integer getSeasonNumber() {
		try {
			return Integer.valueOf(season);
		} catch (NumberFormatException e) {
			return null;
		}
	}
	

	public String getSeriesName() {
		return seriesName;
	}
	

	public String getTitle() {
		return title;
	}
	

	@Override
	public String toString() {
		return EpisodeFormat.getInstance().format(this);
	}
	
}
