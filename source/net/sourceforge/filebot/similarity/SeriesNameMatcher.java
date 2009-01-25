
package net.sourceforge.filebot.similarity;


import static net.sourceforge.filebot.FileBotUtilities.join;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;


public class SeriesNameMatcher {
	
	protected final SeasonEpisodeMatcher seasonEpisodeMatcher = new SeasonEpisodeMatcher();
	
	protected final int threshold;
	
	
	public SeriesNameMatcher(int threshold) {
		if (threshold <= 0)
			throw new IllegalArgumentException("threshold must be greater than 0");
		
		this.threshold = threshold;
	}
	

	public Collection<String> matchAll(List<String> names) {
		SeriesNameCollection seriesNames = new SeriesNameCollection();
		
		// use pattern matching with frequency threshold
		seriesNames.addAll(flatMatchAll(names));
		
		// deep match common word sequences
		seriesNames.addAll(deepMatchAll(names));
		
		return seriesNames;
	}
	

	/**
	 * Try to match and verify all series names using known season episode patterns.
	 * 
	 * @param names list of episode names
	 * @return series names that have been matched one or multiple times depending on the size
	 *         of the given list
	 */
	protected Collection<String> flatMatchAll(Iterable<String> names) {
		ThresholdCollection<String> seriesNames = new ThresholdCollection<String>(threshold, String.CASE_INSENSITIVE_ORDER);
		
		for (String name : names) {
			String match = matchBySeasonEpisodePattern(name);
			
			if (match != null) {
				seriesNames.add(match);
			}
		}
		
		return seriesNames;
	}
	

	/**
	 * Try to match all common word sequences in the given list.
	 * 
	 * @param names list of episode names
	 * @return all common word sequences that have been found
	 */
	protected Collection<String> deepMatchAll(List<String> names) {
		// don't use common word sequence matching for less than 5 names
		if (names.size() < threshold) {
			return Collections.emptySet();
		}
		
		String common = matchByFirstCommonWordSequence(names);
		
		if (common != null) {
			// common word sequence found
			return Collections.singleton(common);
		}
		
		// recursive divide and conquer
		List<String> results = new ArrayList<String>();
		
		if (names.size() >= 2) {
			// split list in two and try to match common word sequence on those
			results.addAll(deepMatchAll(names.subList(0, names.size() / 2)));
			results.addAll(deepMatchAll(names.subList(names.size() / 2, names.size())));
		}
		
		return results;
	}
	

	/**
	 * Try to match a series name from the given episode name using known season episode
	 * patterns.
	 * 
	 * @param name episode name
	 * @return a substring of the given name that ends before the first occurrence of a season
	 *         episode pattern, or null
	 */
	public String matchBySeasonEpisodePattern(String name) {
		int seasonEpisodePosition = seasonEpisodeMatcher.find(name);
		
		if (seasonEpisodePosition > 0) {
			// series name ends at the first season episode pattern
			return normalize(name.substring(0, seasonEpisodePosition));
		}
		
		return null;
	}
	

	/**
	 * Try to match a series name from the first common word sequence.
	 * 
	 * @param names various episode names (5 or more for accurate results)
	 * @return a word sequence all episode names have in common, or null
	 */
	public String matchByFirstCommonWordSequence(Collection<String> names) {
		if (names.size() <= 1) {
			// can't match common sequence from less than two names
			return null;
		}
		
		String[] common = null;
		
		for (String name : names) {
			String[] words = normalize(name).split("\\s+");
			
			if (common == null) {
				// initialize common with current word array
				common = words;
			} else {
				// find common sequence
				common = firstCommonSequence(common, words, String.CASE_INSENSITIVE_ORDER);
				
				if (common == null) {
					// no common sequence
					return null;
				}
			}
		}
		
		// join will return null, if common is null
		return join(common, " ");
	}
	

	protected String normalize(String name) {
		// remove group names (remove any [...])
		name = name.replaceAll("\\[[^\\]]+\\]", "");
		
		// remove special characters
		name = name.replaceAll("[\\p{Punct}\\p{Space}]+", " ");
		
		return name.trim();
	}
	

	protected <T> T[] firstCommonSequence(T[] seq1, T[] seq2, Comparator<T> equalsComparator) {
		for (int i = 0; i < seq1.length; i++) {
			for (int j = 0; j < seq2.length; j++) {
				// common sequence length
				int len = 0;
				
				// iterate over common sequence
				while ((i + len < seq1.length) && (j + len < seq2.length) && (equalsComparator.compare(seq1[i + len], seq2[j + len]) == 0)) {
					len++;
				}
				
				// check if a common sequence was found
				if (len > 0) {
					if (i == 0 && len == seq1.length)
						return seq1;
					
					if (j == 0 && len == seq2.length)
						return seq2;
					
					return Arrays.copyOfRange(seq1, i, i + len);
				}
			}
		}
		
		// no intersection at all
		return null;
	}
	
	
	protected static class SeriesNameCollection extends AbstractCollection<String> {
		
		private final Map<String, String> data = new LinkedHashMap<String, String>();
		
		
		@Override
		public boolean add(String value) {
			String key = value.toLowerCase();
			String current = data.get(key);
			
			// prefer strings with similar upper/lower case ration (e.g. prefer Roswell over roswell) 
			if (current == null || firstCharacterCaseBalance(current) < firstCharacterCaseBalance(value)) {
				data.put(key, value);
				return true;
			}
			
			return false;
		}
		

		protected float firstCharacterCaseBalance(String s) {
			int upper = 0;
			int lower = 0;
			
			Scanner scanner = new Scanner(s); // Scanner has white space delimiter by default
			
			while (scanner.hasNext()) {
				char c = scanner.next().charAt(0);
				
				if (Character.isLowerCase(c))
					lower++;
				else if (Character.isUpperCase(c))
					upper++;
			}
			
			// give upper case characters a slight boost
			return (lower + (upper * 1.01f)) / Math.abs(lower - upper);
		}
		

		@Override
		public boolean contains(Object o) {
			return data.containsKey(o.toString().toLowerCase());
		}
		

		@Override
		public Iterator<String> iterator() {
			return data.values().iterator();
		}
		

		@Override
		public int size() {
			return data.size();
		}
		
	}
	

	protected static class ThresholdCollection<E> extends AbstractCollection<E> {
		
		private final Collection<E> heaven;
		private final Map<E, Collection<E>> limbo;
		
		private final int threshold;
		
		
		public ThresholdCollection(int threshold, Comparator<E> equalityComparator) {
			this.heaven = new ArrayList<E>();
			this.limbo = new TreeMap<E, Collection<E>>(equalityComparator);
			this.threshold = threshold;
		}
		

		@Override
		public boolean add(E e) {
			Collection<E> buffer = limbo.get(e);
			
			if (buffer == null) {
				// initialize buffer
				buffer = new ArrayList<E>(threshold);
				limbo.put(e, buffer);
			}
			
			if (buffer == heaven) {
				// threshold reached
				heaven.add(e);
				return true;
			}
			
			// add element to buffer
			buffer.add(e);
			
			// check if threshold has been reached
			if (buffer.size() >= threshold) {
				heaven.addAll(buffer);
				
				// replace buffer with heaven
				limbo.put(e, heaven);
				return true;
			}
			
			return false;
		};
		

		@Override
		public Iterator<E> iterator() {
			return heaven.iterator();
		}
		

		@Override
		public int size() {
			return heaven.size();
		}
		
	}
	
}
