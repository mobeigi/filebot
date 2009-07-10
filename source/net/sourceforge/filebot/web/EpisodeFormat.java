
package net.sourceforge.filebot.web;


import java.text.FieldPosition;
import java.text.Format;
import java.text.ParseException;
import java.text.ParsePosition;


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
		String[] section = source.substring(pos.getIndex()).split(" - ", 3);
		
		// series name and episode identifier are required
		if (section.length < 2) {
			pos.setErrorIndex(0);
			return null;
		}
		
		// normalize and check
		for (int i = 0; i < section.length; i++) {
			section[i] = section[i].trim();
			
			if (section[i].isEmpty()) {
				pos.setErrorIndex(0);
				return null;
			}
		}
		
		String[] sxe = section[1].split("x", 2);
		
		// series name
		String name = section[0];
		
		// season number and episode number
		String season = (sxe.length == 2) ? sxe[0] : null;
		String episode = (sxe.length == 2) ? sxe[1] : section[1];
		
		// episode title
		String title = (section.length == 3) ? section[2] : null;
		
		// did parse input
		pos.setIndex(source.length());
		return new Episode(name, season, episode, title);
	}
	

	@Override
	public Episode parseObject(String source) throws ParseException {
		return (Episode) super.parseObject(source);
	}
	
}
