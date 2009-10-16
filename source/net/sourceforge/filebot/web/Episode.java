
package net.sourceforge.filebot.web;


import java.io.Serializable;
import java.util.Arrays;


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
			return new Integer(episode);
		} catch (NumberFormatException e) {
			return null;
		}
	}
	

	public String getSeason() {
		return season;
	}
	

	public Integer getSeasonNumber() {
		try {
			return new Integer(season);
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
	public boolean equals(Object obj) {
		if (obj instanceof Episode) {
			Episode other = (Episode) obj;
			return equals(season, other.season) && equals(episode, other.episode) && equals(seriesName, other.seriesName) && equals(title, other.title);
		}
		
		return false;
	}
	

	private boolean equals(Object o1, Object o2) {
		if (o1 == null || o2 == null)
			return o1 == o2;
		
		return o1.equals(o2);
	}
	

	@Override
	public int hashCode() {
		return Arrays.hashCode(new Object[] { seriesName, season, episode, title });
	}
	

	@Override
	public String toString() {
		return EpisodeFormat.getInstance().format(this);
	}
	
}
