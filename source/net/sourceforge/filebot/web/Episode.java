
package net.sourceforge.filebot.web;


import java.io.Serializable;


public class Episode implements Serializable {
	
	private String showName;
	private String seasonNumber;
	private String episodeNumber;
	private String title;
	
	
	public Episode(String showName, String seasonNumber, String episodeNumber, String title) {
		this.showName = showName;
		this.seasonNumber = seasonNumber;
		this.episodeNumber = episodeNumber;
		this.title = title;
	}
	

	public Episode(String showName, String episodeNumber, String title) {
		this(showName, null, episodeNumber, title);
	}
	

	public String getEpisodeNumber() {
		return episodeNumber;
	}
	

	public String getSeasonNumber() {
		return seasonNumber;
	}
	

	public String getShowName() {
		return showName;
	}
	

	public String getTitle() {
		return title;
	}
	

	public void setShowName(String seriesName) {
		this.showName = seriesName;
	}
	

	public void setSeasonNumber(String seasonNumber) {
		this.seasonNumber = seasonNumber;
	}
	

	public void setEpisodeNumber(String episodeNumber) {
		this.episodeNumber = episodeNumber;
	}
	

	public void setTitle(String episodeName) {
		this.title = episodeName;
	}
	

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(40);
		
		sb.append(showName);
		sb.append(" - ");
		
		if (seasonNumber != null)
			sb.append(seasonNumber + "x");
		
		sb.append(episodeNumber);
		
		sb.append(" - ");
		sb.append(title);
		
		return sb.toString();
	}
}
