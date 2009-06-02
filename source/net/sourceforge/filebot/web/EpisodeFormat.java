
package net.sourceforge.filebot.web;


import java.text.FieldPosition;
import java.text.Format;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class EpisodeFormat extends Format {
	
	private static final EpisodeFormat instance = new EpisodeFormat();
	

	public static EpisodeFormat getInstance() {
		return instance;
	}
	

	@Override
	public StringBuffer format(Object obj, StringBuffer sb, FieldPosition pos) {
		Episode episode = (Episode) obj;
		
		// try to format episode number, or use episode "number" string as is
		String episodeNumber = (episode.getEpisodeNumber() != null ? String.format("%02d", episode.getEpisodeNumber()) : episode.getEpisode());
		
		// series name should not be empty or null
		sb.append(episode.getSeriesName());
		
		if (episode.getSeason() != null) {
			// season and episode
			sb.append(" - ").append(episode.getSeason()).append('x').append(episodeNumber);
		} else if (episodeNumber != null) {
			// episode, but no season
			sb.append(" - ").append(episodeNumber);
		}
		
		if (episode.getTitle() != null) {
			sb.append(" - ").append(episode.getTitle());
		}
		
		return sb;
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
