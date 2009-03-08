
package net.sourceforge.filebot.web;


import java.io.Serializable;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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
	

	@Override
	public String toString() {
		return EpisodeFormat.getInstance().format(this);
	}
	
	
	public static class EpisodeFormat extends Format {
		
		private static final EpisodeFormat instance = new EpisodeFormat();
		
		
		public static EpisodeFormat getInstance() {
			return instance;
		}
		

		@Override
		public StringBuffer format(Object obj, StringBuffer sb, FieldPosition pos) {
			Episode episode = (Episode) obj;
			
			sb.append(episode.getSeriesName()).append(" - ");
			
			if (episode.getSeasonNumber() != null) {
				sb.append(episode.getSeasonNumber()).append('x');
			}
			
			sb.append(formatEpisodeNumber(episode.getEpisodeNumber()));
			
			return sb.append(" - ").append(episode.getTitle());
		}
		

		protected String formatEpisodeNumber(String number) {
			if (number.length() < 2) {
				try {
					return String.format("%02d", Integer.parseInt(number));
				} catch (NumberFormatException e) {
					// ignore
				}
			}
			
			return number;
		}
		

		@Override
		public Episode parseObject(String source, ParsePosition pos) {
			Pattern pattern = Pattern.compile("(.*) - (?:(\\w+?)x)?(\\w+)? - (.*)");
			
			Matcher matcher = pattern.matcher(source).region(pos.getIndex(), source.length());
			
			if (!matcher.matches()) {
				pos.setErrorIndex(matcher.regionStart());
				return null;
			}
			
			// episode number must not be null
			if (matcher.group(3) == null) {
				pos.setErrorIndex(matcher.start(3));
				return null;
			}
			
			pos.setIndex(matcher.end());
			return new Episode(matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4));
		}
		

		@Override
		public Episode parseObject(String source) throws ParseException {
			return (Episode) super.parseObject(source);
		}
		
	}
	
}
