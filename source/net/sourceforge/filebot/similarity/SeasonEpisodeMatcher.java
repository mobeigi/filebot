
package net.sourceforge.filebot.similarity;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SeasonEpisodeMatcher {
	
	private final SeasonEpisodePattern[] patterns;
	

	public SeasonEpisodeMatcher() {
		patterns = new SeasonEpisodePattern[3];
		
		// match patterns like S01E01, s01e02, ... [s01]_[e02], s01.e02, s01e02a, ...
		patterns[0] = new SeasonEpisodePattern("(?<!\\p{Alnum})[Ss](\\d{1,2})[^\\p{Alnum}]{0,3}[Ee](\\d{1,3})(?!\\p{Digit})");
		
		// match patterns like 1x01, 1.02, ..., 1x01a, 10x01, 10.02, ...
		patterns[1] = new SeasonEpisodePattern("(?<!\\p{Alnum})(\\d{1,2})[x.](\\d{1,3})(?!\\p{Digit})");
		
		// match patterns like 01, 102, 1003 (enclosed in separators)
		patterns[2] = new SeasonEpisodePattern("(?<=^|[._ ])([0-1]?\\d?)(\\d{2})(?=[._ ]|$)") {
			
			@Override
			protected Collection<SxE> process(MatchResult match) {
				// interpret match as season and episode
				SxE seasonEpisode = new SxE(match.group(1), match.group(2));
				
				// interpret match as episode number only
				SxE episodeOnly = new SxE(null, match.group(1) + match.group(2));
				
				return Arrays.asList(seasonEpisode, episodeOnly);
			}
		};
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
	

	public static class SxE {
		
		public static final int UNDEFINED = -1;
		
		public final int season;
		public final int episode;
		

		public SxE(int season, int episode) {
			this.season = season;
			this.episode = episode;
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
			return String.format("%dx%02d", season, episode);
		}
	}
	

	protected static class SeasonEpisodePattern {
		
		protected final Pattern pattern;
		

		public SeasonEpisodePattern(String pattern) {
			this.pattern = Pattern.compile(pattern);
		}
		

		protected Collection<SxE> process(MatchResult match) {
			return Collections.singleton(new SxE(match.group(1), match.group(2)));
		}
		

		public List<SxE> match(CharSequence name) {
			// name will probably contain no more than two matches
			List<SxE> matches = new ArrayList<SxE>(2);
			
			Matcher matcher = pattern.matcher(name);
			
			while (matcher.find()) {
				matches.addAll(process(matcher));
			}
			
			return matches;
		}
		

		public int find(CharSequence name, int fromIndex) {
			Matcher matcher = pattern.matcher(name);
			
			if (matcher.find(fromIndex))
				return matcher.start();
			
			return -1;
		}
	}
	
}
