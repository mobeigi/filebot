
package net.sourceforge.filebot.web;


public class Episode {
	
	private String showname;
	
	private String numberOfSeason;
	
	private String numberOfEpisode;
	
	private String title;
	
	
	public Episode(String showname, String numberOfSeason, String numberOfEpisode, String title) {
		this.showname = showname;
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
	

	public void setNumberOfEpisode(String numberOfEpisode) {
		this.numberOfEpisode = numberOfEpisode;
	}
	

	public String getNumberOfSeason() {
		return numberOfSeason;
	}
	

	public void setNumberOfSeason(String numberOfSeason) {
		this.numberOfSeason = numberOfSeason;
	}
	

	public String getShowname() {
		return showname;
	}
	

	public void setShowname(String showname) {
		this.showname = showname;
	}
	

	public String getTitle() {
		return title;
	}
	

	public void setTitle(String title) {
		this.title = title;
	}
	

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		
		sb.append(showname + " - ");
		
		if (numberOfSeason != null)
			sb.append(numberOfSeason + "x");
		
		sb.append(numberOfEpisode);
		
		sb.append(" - " + title);
		
		return sb.toString();
	}
	
}
