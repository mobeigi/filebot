
package net.sourceforge.filebot.web;


public class SeasonOutOfBoundsException extends IndexOutOfBoundsException {
	
	private final String seriesName;
	private final int season;
	private final int maxSeason;
	
	
	public SeasonOutOfBoundsException(String seriesName, int season, int maxSeason) {
		this.seriesName = seriesName;
		this.season = season;
		this.maxSeason = maxSeason;
	}
	

	@Override
	public String getMessage() {
		return String.format("%s has only %d season%s.", seriesName, maxSeason, maxSeason != 1 ? "s" : "");
	}
	

	public String getSeriesName() {
		return seriesName;
	}
	

	public int getSeason() {
		return season;
	}
	

	public int getMaxSeason() {
		return maxSeason;
	}
	
}
