
package net.sourceforge.filebot.web;


import java.io.Serializable;
import java.util.Arrays;


public class Episode implements Serializable {
	
	private String seriesName;
	private String season;
	private String episode;
	private String title;
	
	// special number
	private String special;
	
	// episode airdate
	private Date airdate;
	

	protected Episode() {
		// used by serializer
	}
	

	public Episode(String seriesName, String season, String episode, String title) {
		this(seriesName, season, episode, title, null, null);
	}
	

	public Episode(String seriesName, String season, String episode, String title, String special, Date airdate) {
		this.seriesName = seriesName;
		this.season = season;
		this.episode = episode;
		this.title = title;
		this.special = special;
		this.airdate = airdate;
	}
	

	public String getSeriesName() {
		return seriesName;
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
	

	public String getTitle() {
		return title;
	}
	

	public String getSpecial() {
		return special;
	}
	

	public Integer getSpecialNumber() {
		try {
			return new Integer(special);
		} catch (NumberFormatException e) {
			return null;
		}
	}
	

	public Date airdate() {
		return airdate;
	}
	

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Episode) {
			Episode other = (Episode) obj;
			return equals(season, other.season) && equals(episode, other.episode) && equals(seriesName, other.seriesName) && equals(title, other.title) && equals(special, other.special);
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
		return Arrays.hashCode(new Object[] { seriesName, season, episode, title, special });
	}
	

	@Override
	public String toString() {
		return EpisodeFormat.getInstance().format(this);
	}
	
}
