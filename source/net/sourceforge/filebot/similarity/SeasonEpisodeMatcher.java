
package net.sourceforge.filebot.similarity;


import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SeasonEpisodeMatcher {
	
	private final SeasonEpisodePattern[] patterns;
	
	
	public SeasonEpisodeMatcher() {
		patterns = new SeasonEpisodePattern[3];
		
		// match patterns like S01E01, s01e02, ... [s01]_[e02], s01.e02, ...
		patterns[0] = new SeasonEpisodePattern("(?<!\\p{Alnum})[Ss](\\d{1,2})[^\\p{Alnum}]{0,3}[Ee](\\d{1,3})(?!\\p{Digit})");
		
		// match patterns like 1x01, 1x02, ... 10x01, 10x02, ...
		patterns[1] = new SeasonEpisodePattern("(?<!\\p{Alnum})(\\d{1,2})x(\\d{1,3})(?!\\p{Digit})");
		
		// match patterns like 01, 102, 1003 (enclosed in separators)
		patterns[2] = new SeasonEpisodePattern("(?<=^|[\\._ ])([0-1]?\\d?)(\\d{2})(?=[\\._ ]|$)");
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
	

	public int find(CharSequence name) {
		for (SeasonEpisodePattern pattern : patterns) {
			int index = pattern.find(name);
			
			if (index >= 0) {
				// current pattern did match
				return index;
			}
		}
		
		return -1;
	}
	
	
	public static class SxE {
		
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
			return number == null || number.isEmpty() ? 0 : Integer.parseInt(number);
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
		public String toString() {
			return String.format("%dx%02d", season, episode);
		}
	}
	

	protected static class SeasonEpisodePattern {
		
		protected final Pattern pattern;
		
		protected final int seasonGroup;
		protected final int episodeGroup;
		
		
		public SeasonEpisodePattern(String pattern) {
			this(Pattern.compile(pattern), 1, 2);
		}
		

		public SeasonEpisodePattern(Pattern pattern, int seasonGroup, int episodeGroup) {
			this.pattern = pattern;
			this.seasonGroup = seasonGroup;
			this.episodeGroup = episodeGroup;
		}
		

		public List<SxE> match(CharSequence name) {
			// name will probably contain no more than one match, but may contain more
			List<SxE> matches = new ArrayList<SxE>(1);
			
			Matcher matcher = pattern.matcher(name);
			
			while (matcher.find()) {
				matches.add(new SxE(matcher.group(seasonGroup), matcher.group(episodeGroup)));
			}
			
			return matches;
		}
		

		public int find(CharSequence name) {
			Matcher matcher = pattern.matcher(name);
			
			if (matcher.find())
				return matcher.start();
			
			return -1;
		}
	}
	
}
