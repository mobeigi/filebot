
package net.sourceforge.filebot.web;


public class SeasonOutOfBoundsException extends IndexOutOfBoundsException {
	
	private final String showName;
	private final int season;
	private final int maxSeason;
	
	
	public SeasonOutOfBoundsException(String showName, int season, int maxSeason) {
		this.showName = showName;
		this.season = season;
		this.maxSeason = maxSeason;
	}
	

	@Override
	public String getMessage() {
		return String.format("%s has only %d season%s.", showName, maxSeason, maxSeason != 1 ? "s" : "");
	}
	

	public String getShowName() {
		return showName;
	}
	

	public int getSeason() {
		return season;
	}
	

	public int getMaxSeason() {
		return maxSeason;
	}
	
}
