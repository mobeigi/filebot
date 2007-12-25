
package net.sourceforge.filebot.web;


public class Episode {
	
	private String showName;
	private String numberOfSeason;
	private String numberOfEpisode;
	private String title;
	
	
	public Episode(String showname, String numberOfSeason, String numberOfEpisode, String title) {
		this.showName = showname;
		this.numberOfSeason = numberOfSeason;
		this.numberOfEpisode = numberOfEpisode;
		this.title = title;
	}
	

	public Episode(String showname, String numberOfEpisode, String title) {
		this(showname, null, numberOfEpisode, title);
	}
	

	public String getNumberOfEpisode() {
		return numberOfEpisode;
	}
	

	public String getNumberOfSeason() {
		return numberOfSeason;
	}
	

	public String getShowName() {
		return showName;
	}
	

	public String getTitle() {
		return title;
	}
	

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		
		sb.append(showName + " - ");
		
		if (numberOfSeason != null)
			sb.append(numberOfSeason + "x");
		
		sb.append(numberOfEpisode);
		
		sb.append(" - " + title);
		
		return sb.toString();
	}
	
}
