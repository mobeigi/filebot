
package net.sourceforge.filebot.web;


import java.io.Serializable;
import java.text.NumberFormat;


public class Episode implements Serializable {
	
	private String seriesName;
	private String seasonNumber;
	private String episodeNumber;
	private String title;
	
	
	public Episode(String seriesName, String seasonNumber, String episodeNumber, String title) {
		this.seriesName = seriesName;
		this.seasonNumber = seasonNumber;
		this.episodeNumber = episodeNumber;
		this.title = title;
	}
	

	public Episode(String seriesName, String episodeNumber, String title) {
		this(seriesName, null, episodeNumber, title);
	}
	

	public String getEpisodeNumber() {
		return episodeNumber;
	}
	

	public String getSeasonNumber() {
		return seasonNumber;
	}
	

	public String getSeriesName() {
		return seriesName;
	}
	

	public String getTitle() {
		return title;
	}
	

	public void setSeriesName(String seriesName) {
		this.seriesName = seriesName;
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
		
		sb.append(seriesName).append(" - ");
		
		if (seasonNumber != null) {
			sb.append(seasonNumber).append("x");
		}
		
		sb.append(episodeNumber).append(" - ").append(title);
		
		return sb.toString();
	}
	

	public static <T extends Iterable<Episode>> T formatEpisodeNumbers(T episodes, int minDigits) {
		// find max. episode number length
		for (Episode episode : episodes) {
			try {
				String episodeNumber = episode.getEpisodeNumber();
				
				if (episodeNumber.length() > minDigits && Integer.parseInt(episodeNumber) > 0) {
					minDigits = episodeNumber.length();
				}
			} catch (NumberFormatException e) {
				// ignore
			}
		}
		
		// pad episode numbers with zeros (e.g. %02d) so all episode numbers have the same number of digits
		NumberFormat numberFormat = NumberFormat.getIntegerInstance();
		numberFormat.setMinimumIntegerDigits(minDigits);
		numberFormat.setGroupingUsed(false);
		
		for (Episode episode : episodes) {
			try {
				episode.setEpisodeNumber(numberFormat.format(Integer.parseInt(episode.getEpisodeNumber())));
			} catch (NumberFormatException e) {
				// ignore
			}
		}
		
		return episodes;
	}
}
