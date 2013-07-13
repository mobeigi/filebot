
package net.sourceforge.filebot.web;


import static net.sourceforge.tuned.StringUtilities.*;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class EpisodeFormat extends Format {
	
	public static final EpisodeFormat SeasonEpisode = new EpisodeFormat(true, false);
	
	private final boolean includeAirdate;
	private final boolean includeSpecial;
	
	
	public EpisodeFormat(boolean includeSpecial, boolean includeAirdate) {
		this.includeSpecial = includeSpecial;
		this.includeAirdate = includeAirdate;
	}
	
	
	@Override
	public StringBuffer format(Object obj, StringBuffer sb, FieldPosition pos) {
		if (obj instanceof MultiEpisode) {
			return sb.append(formatMultiEpisode(((MultiEpisode) obj).getEpisodes()));
		}
		
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
			if (episode.getEpisode() != null) {
				sb.append(" - ").append(episodeNumber);
			} else if (includeSpecial && episode.getSpecial() != null) {
				sb.append(" - ").append("Special " + episode.getSpecial());
			}
		}
		
		sb.append(" - ").append(episode.getTitle());
		
		if (includeAirdate && episode.getAirdate() != null) {
			sb.append(" [").append(episode.getAirdate().format("yyyy-MM-dd")).append("]");
		}
		
		return sb;
	}
	
	
	public String formatSxE(Episode episode) {
		if (episode instanceof MultiEpisode) {
			return formatMultiSxE(((MultiEpisode) episode).getEpisodes());
		}
		
		StringBuilder sb = new StringBuilder();
		
		if (episode.getSeason() != null || episode.getSpecial() != null) {
			sb.append(episode.getSpecial() == null ? episode.getSeason() : 0).append('x');
		}
		
		if (episode.getEpisode() != null || episode.getSpecial() != null) {
			sb.append(String.format("%02d", episode.getSpecial() == null ? episode.getEpisode() : episode.getSpecial()));
		}
		
		return sb.toString();
	}
	
	
	public String formatS00E00(Episode episode) {
		if (episode instanceof MultiEpisode) {
			return formatMultiS00E00(((MultiEpisode) episode).getEpisodes());
		}
		
		StringBuilder sb = new StringBuilder();
		
		if (episode.getSeason() != null || episode.getSpecial() != null) {
			sb.append(String.format("S%02d", episode.getSpecial() == null ? episode.getSeason() : 0));
		}
		
		if (episode.getEpisode() != null || episode.getSpecial() != null) {
			sb.append(String.format("E%02d", episode.getSpecial() == null ? episode.getEpisode() : episode.getSpecial()));
		}
		
		return sb.toString();
	}
	
	
	public String formatMultiEpisode(Iterable<Episode> episodes) {
		Set<String> name = new LinkedHashSet<String>();
		Set<String> sxe = new LinkedHashSet<String>();
		Set<String> title = new LinkedHashSet<String>();
		for (Episode it : episodes) {
			name.add(it.getSeriesName());
			sxe.add(formatSxE(it));
			title.add(it.getTitle().replaceAll("[(]([^)]*)[)]$", "").trim());
		}
		
		return String.format("%s - %s - %s", join(name, " & "), join(sxe, " & "), join(title, " & "));
	}
	
	
	public String formatMultiSxE(Iterable<Episode> episodes) {
		StringBuilder sb = new StringBuilder();
		Integer ps = null;
		for (Episode it : episodes) {
			if (!it.getSeason().equals(ps)) {
				if (sb.length() > 0) {
					sb.append(' ');
				}
				sb.append(it.getSeason()).append('x').append(String.format("%02d", it.getEpisode()));
			} else {
				sb.append('-').append(String.format("%02d", it.getEpisode()));
			}
			ps = it.getSeason();
		}
		
		return sb.toString();
	}
	
	
	public String formatMultiS00E00(Iterable<Episode> episodes) {
		StringBuilder sb = new StringBuilder();
		Integer ps = null;
		for (Episode it : episodes) {
			if (sb.length() > 0) {
				sb.append("-");
			}
			if (!it.getSeason().equals(ps)) {
				sb.append(String.format("S%02d", it.getSeason())).append(String.format("E%02d", it.getEpisode()));
			} else {
				sb.append(String.format("E%02d", it.getEpisode()));
			}
			ps = it.getSeason();
		}
		
		return sb.toString();
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
			return new Episode(name, null, season, episode, title, season == null ? episode : null, special, airdate, null);
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
