
package net.sourceforge.filebot.web;


import java.text.FieldPosition;
import java.text.Format;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class EpisodeFormat extends Format {
	
	private boolean includeAirdate = true;
	private boolean includeSpecial = true;
	

	public static EpisodeFormat getSeasonEpisodeInstance() {
		return new EpisodeFormat(true, false);
	}
	

	public static EpisodeFormat getDefaultInstance() {
		return new EpisodeFormat(true, true);
	}
	

	public EpisodeFormat(boolean includeSpecial, boolean includeAirdate) {
		this.includeSpecial = includeSpecial;
		this.includeAirdate = includeAirdate;
	}
	

	@Override
	public StringBuffer format(Object obj, StringBuffer sb, FieldPosition pos) {
		// format episode object, e.g. Dark Angel - 3x01 - Labyrinth [2009-06-01]
		Episode episode = (Episode) obj;
		
		// episode number is most likely a number but could also be some kind of special identifier (e.g. Special)
		String episodeNumber = episode.getEpisode() != null ? String.format("%02d", episode.getEpisode()) : null;
		
		// series name should not be empty or null
		sb.append(episode.getSeriesName());
		
		if (episode.getSeason() != null) {
			// season and episode
			sb.append(" - ").append(episode.getSeason()).append('x');
			
			if (episode.getEpisode() != null) {
				sb.append(String.format("%02d", episode.getEpisode()));
			} else if (includeSpecial && episode.getSpecial() != null) {
				sb.append("Special " + episode.getSpecial());
			}
		} else {
			// episode, but no season
			sb.append(" - ").append(episodeNumber);
		}
		
		sb.append(" - ").append(episode.getTitle());
		
		if (includeAirdate && episode.airdate() != null) {
			sb.append(" [").append(episode.airdate().format("yyyy-MM-dd")).append("]");
		}
		
		return sb;
	}
	

	private final Pattern sxePattern = Pattern.compile("- (?:(\\d{1,2})x)?(Special )?(\\d{1,3}) -");
	private final Pattern airdatePattern = Pattern.compile("\\[(\\d{4}-\\d{1,2}-\\d{1,2})\\]");
	

	@Override
	public Episode parseObject(String s, ParsePosition pos) {
		StringBuilder source = new StringBuilder(s);
		
		Integer season = null;
		Integer episode = null;
		Integer special = null;
		Date airdate = null;
		
		Matcher m;
		
		if ((m = airdatePattern.matcher(source)).find()) {
			airdate = Date.parse(m.group(1), "yyyy-MM-dd");
			source.replace(m.start(), m.end(), ""); // remove matched part from text
		}
		
		if ((m = sxePattern.matcher(source)).find()) {
			season = (m.group(1) == null) ? null : new Integer(m.group(1));
			if (m.group(2) == null)
				episode = new Integer(m.group(3));
			else
				special = new Integer(m.group(3));
			
			source.replace(m.start(), m.end(), ""); // remove matched part from text
			
			// assume that all the remaining text is series name and title
			String name = source.substring(0, m.start()).trim();
			String title = source.substring(m.start()).trim();
			
			// did parse input
			pos.setIndex(source.length());
			return new Episode(name, season, episode, title, season == null ? episode : null, special, airdate);
		}
		
		// failed to parse input
		pos.setErrorIndex(0);
		return null;
	}
	

	@Override
	public Episode parseObject(String source) throws ParseException {
		return (Episode) super.parseObject(source);
	}
	
}
