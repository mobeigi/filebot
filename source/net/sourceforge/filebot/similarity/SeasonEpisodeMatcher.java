
package net.sourceforge.filebot.similarity;


import static java.util.Arrays.*;
import static java.util.Collections.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SeasonEpisodeMatcher {
	
	public static final SeasonEpisodeFilter DEFAULT_SANITY = new SeasonEpisodeFilter(50, 50, 1000);
	
	private SeasonEpisodePattern[] patterns;
	
	
	public SeasonEpisodeMatcher(SeasonEpisodeFilter sanity, boolean strict) {
		patterns = new SeasonEpisodePattern[5];
		
		// match patterns like Season 01 Episode 02, ...
		patterns[0] = new SeasonEpisodePattern(null, "(?<!\\p{Alnum})(?i:season|series)[^\\p{Alnum}]{0,3}(\\d{1,4})[^\\p{Alnum}]{0,3}(?i:episode)[^\\p{Alnum}]{0,3}(\\d{1,4})[^\\p{Alnum}]{0,3}(?!\\p{Digit})");
		
		// match patterns like S01E01, s01e02, ... [s01]_[e02], s01.e02, s01e02a, s2010e01 ... s01e01-02-03-04, [s01]_[e01-02-03-04] ...
		patterns[1] = new SeasonEpisodePattern(null, "(?<!\\p{Digit})[Ss](\\d{1,2}|\\d{4})[^\\p{Alnum}]{0,3}[Ee][Pp]?(((?<=[^._ ])[Ee]?[Pp]?\\d{1,3}(\\D|$))+)") {
			
			@Override
			protected Collection<SxE> process(MatchResult match) {
				List<SxE> matches = new ArrayList<SxE>(2);
				Scanner epno = new Scanner(match.group(2)).useDelimiter("\\D+");
				while (epno.hasNext()) {
					matches.add(new SxE(match.group(1), epno.next()));
				}
				return matches;
			}
		};
		
		// match patterns like 1x01, 1.02, ..., 1x01a, 10x01, 10.02, ... 1x01-02-03-04, 1x01x02x03x04 ...
		patterns[2] = new SeasonEpisodePattern(sanity, "(?<!\\p{Alnum}|\\d{4}[.])(\\d{1,2})[xe.](((?<=[^._ ])\\d{2,3}(\\D|$))+)") {
			
			@Override
			protected Collection<SxE> process(MatchResult match) {
				List<SxE> matches = new ArrayList<SxE>(2);
				Scanner epno = new Scanner(match.group(2)).useDelimiter("\\D+");
				while (epno.hasNext()) {
					matches.add(new SxE(match.group(1), epno.next()));
				}
				return matches;
			}
		};
		
		// match patterns like ep1, ep.1, ...
		patterns[3] = new SeasonEpisodePattern(sanity, "(?<!\\p{Alnum})(?i:e|ep|episode)[^\\p{Alnum}]{0,3}(\\d{1,3})(?!\\p{Digit})") {
			
			@Override
			protected Collection<SxE> process(MatchResult match) {
				// regex doesn't match season
				return singleton(new SxE(null, match.group(1)));
			}
		};
		
		// match patterns like 01, 102, 1003 (enclosed in separators)
		patterns[4] = new SeasonEpisodePattern(sanity, "(?<!\\p{Alnum})([0-1]?\\d?)(\\d{2})(?!\\p{Alnum})") {
			
			@Override
			protected Collection<SxE> process(MatchResult match) {
				// interpret match as season and episode
				SxE seasonEpisode = new SxE(match.group(1), match.group(2));
				
				// interpret match as episode number only
				SxE absoluteEpisode = new SxE(null, match.group(1) + match.group(2));
				
				// return both matches, unless they are one and the same
				return seasonEpisode.equals(absoluteEpisode) ? singleton(seasonEpisode) : asList(seasonEpisode, absoluteEpisode);
			}
		};
		
		// only use S00E00 and SxE pattern in strict mode
		if (strict) {
			patterns = new SeasonEpisodePattern[] { patterns[0], patterns[1], patterns[2] };
		}
	}
	
	
	/**
	 * Try to get season and episode numbers for the given string.
	 * 
	 * @param name match this string against the a set of know patterns
	 * @return the matches returned by the first pattern that returns any matches for this
	 *         string, or null if no pattern returned any matches
	 */
	public List<SxE> match(CharSequence name) {
		for (SeasonEpisodePattern pattern : patterns) {
			List<SxE> match = pattern.match(name);
			
			if (!match.isEmpty()) {
				// current pattern did match
				return match;
			}
		}
		
		return null;
	}
	
	
	public int find(CharSequence name, int fromIndex) {
		for (SeasonEpisodePattern pattern : patterns) {
			int index = pattern.find(name, fromIndex);
			
			if (index >= 0) {
				// current pattern did match
				return index;
			}
		}
		
		return -1;
	}
	
	
	public Matcher matcher(CharSequence name) {
		for (SeasonEpisodePattern pattern : patterns) {
			Matcher matcher = pattern.matcher(name);
			
			// check if current pattern matches 
			if (matcher.find()) {
				// reset matcher state
				return matcher.reset();
			}
		}
		
		return null;
	}
	
	
	public static class SxE {
		
		public static final int UNDEFINED = -1;
		
		public final int season;
		public final int episode;
		
		
		public SxE(Integer season, Integer episode) {
			this.season = season != null ? season : UNDEFINED;
			this.episode = episode != null ? episode : UNDEFINED;
		}
		
		
		public SxE(String season, String episode) {
			this.season = parse(season);
			this.episode = parse(episode);
		}
		
		
		protected int parse(String number) {
			try {
				return Integer.parseInt(number);
			} catch (Exception e) {
				return UNDEFINED;
			}
		}
		
		
		@Override
		public boolean equals(Object object) {
			if (object instanceof SxE) {
				SxE other = (SxE) object;
				return this.season == other.season && this.episode == other.episode;
			}
			
			return false;
		}
		
		
		@Override
		public int hashCode() {
			return Arrays.hashCode(new Object[] { season, episode });
		}
		
		
		@Override
		public String toString() {
			return season >= 0 ? String.format("%dx%02d", season, episode) : String.format("%02d", episode);
		}
	}
	
	
	public static class SeasonEpisodeFilter {
		
		public final int seasonLimit;
		public final int seasonEpisodeLimit;
		public final int absoluteEpisodeLimit;
		
		
		public SeasonEpisodeFilter(int seasonLimit, int seasonEpisodeLimit, int absoluteEpisodeLimit) {
			this.seasonLimit = seasonLimit;
			this.seasonEpisodeLimit = seasonEpisodeLimit;
			this.absoluteEpisodeLimit = absoluteEpisodeLimit;
		}
		
		
		boolean filter(SxE sxe) {
			return (sxe.season >= 0 && sxe.season < seasonLimit && sxe.episode < seasonEpisodeLimit) || (sxe.season < 0 && sxe.episode < absoluteEpisodeLimit);
		}
	}
	
	
	public static class SeasonEpisodePattern {
		
		protected final Pattern pattern;
		protected final SeasonEpisodeFilter sanity;
		
		
		public SeasonEpisodePattern(SeasonEpisodeFilter sanity, String pattern) {
			this.pattern = Pattern.compile(pattern);
			this.sanity = sanity;
		}
		
		
		public Matcher matcher(CharSequence name) {
			return pattern.matcher(name);
		}
		
		
		protected Collection<SxE> process(MatchResult match) {
			return singleton(new SxE(match.group(1), match.group(2)));
		}
		
		
		public List<SxE> match(CharSequence name) {
			// name will probably contain no more than two matches
			List<SxE> matches = new ArrayList<SxE>(2);
			
			Matcher matcher = matcher(name);
			
			while (matcher.find()) {
				for (SxE value : process(matcher)) {
					if (sanity == null || sanity.filter(value)) {
						matches.add(value);
					}
				}
			}
			
			return matches;
		}
		
		
		public int find(CharSequence name, int fromIndex) {
			Matcher matcher = matcher(name).region(fromIndex, name.length());
			
			while (matcher.find()) {
				for (SxE value : process(matcher)) {
					if (sanity == null || sanity.filter(value)) {
						return matcher.start();
					}
				}
			}
			
			return -1;
		}
	}
	
}
