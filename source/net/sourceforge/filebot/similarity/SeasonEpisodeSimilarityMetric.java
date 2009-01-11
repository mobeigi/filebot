
package net.sourceforge.filebot.similarity;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SeasonEpisodeSimilarityMetric implements SimilarityMetric {
	
	private final NumericSimilarityMetric fallbackMetric = new NumericSimilarityMetric();
	
	private final SeasonEpisodePattern[] patterns;
	
	
	public SeasonEpisodeSimilarityMetric() {
		patterns = new SeasonEpisodePattern[3];
		
		// match patterns like S01E01, s01e02, ... [s01]_[e02], s01.e02, ...
		patterns[0] = new SeasonEpisodePattern("(?<!\\p{Alnum})[Ss](\\d{1,2})[^\\p{Alnum}]{0,3}[Ee](\\d{1,3})(?!\\p{Digit})");
		
		// match patterns like 1x01, 1x02, ... 10x01, 10x02, ...
		patterns[1] = new SeasonEpisodePattern("(?<!\\p{Alnum})(\\d{1,2})x(\\d{1,3})(?!\\p{Digit})");
		
		// match patterns like 01, 102, 1003 (enclosed in separators)
		patterns[2] = new SeasonEpisodePattern("(?<=^|[\\._ ])([0-2]?\\d?)(\\d{2})(?=[\\._ ]|$)");
	}
	

	@Override
	public float getSimilarity(Object o1, Object o2) {
		List<SxE> sxeVector1 = match(normalize(o1));
		List<SxE> sxeVector2 = match(normalize(o2));
		
		if (sxeVector1 == null || sxeVector2 == null) {
			// name does not match any known pattern, return numeric similarity
			return fallbackMetric.getSimilarity(o1, o2);
		}
		
		if (Collections.disjoint(sxeVector1, sxeVector2)) {
			// vectors have no episode matches in common 
			return 0;
		}
		
		// vectors have at least one episode match in common
		return 1;
	}
	

	/**
	 * Try to get season and episode numbers for the given string.
	 * 
	 * @param name match this string against the a set of know patterns
	 * @return the matches returned by the first pattern that returns any matches for this
	 *         string, or null if no pattern returned any matches
	 */
	protected List<SxE> match(String name) {
		for (SeasonEpisodePattern pattern : patterns) {
			List<SxE> match = pattern.match(name);
			
			if (!match.isEmpty()) {
				// current pattern did match
				return match;
			}
		}
		
		return null;
	}
	

	protected String normalize(Object object) {
		return object.toString();
	}
	

	@Override
	public String getDescription() {
		return "Similarity of season and episode numbers";
	}
	

	@Override
	public String getName() {
		return "Season and Episode";
	}
	

	@Override
	public String toString() {
		return getClass().getName();
	}
	
	
	protected static class SxE {
		
		public final int season;
		public final int episode;
		
		
		public SxE(int season, int episode) {
			this.season = season;
			this.episode = episode;
		}
		

		public SxE(String season, String episode) {
			this(parseNumber(season), parseNumber(episode));
		}
		

		private static int parseNumber(String number) {
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
		

		public List<SxE> match(String name) {
			// name will probably contain no more than one match, but may contain more
			List<SxE> matches = new ArrayList<SxE>(1);
			
			Matcher matcher = pattern.matcher(name);
			
			while (matcher.find()) {
				matches.add(new SxE(matcher.group(seasonGroup), matcher.group(episodeGroup)));
			}
			
			return matches;
		}
	}
	
}
