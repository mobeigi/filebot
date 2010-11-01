
package net.sourceforge.filebot.similarity;


import static java.util.Collections.*;
import static net.sourceforge.tuned.StringUtilities.*;

import java.io.File;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.filebot.similarity.SeasonEpisodeMatcher.SxE;
import net.sourceforge.tuned.FileUtilities;


public class SeriesNameMatcher {
	
	protected final SeasonEpisodeMatcher seasonEpisodeMatcher = new SeasonEpisodeMatcher();
	protected final NameSimilarityMetric nameSimilarityMetric = new NameSimilarityMetric();
	
	protected final int commonWordSequenceMaxStartIndex = 3;
	

	public Collection<String> matchAll(File[] files) {
		SeriesNameCollection seriesNames = new SeriesNameCollection();
		
		// group files by parent folder
		for (Entry<File, String[]> entry : mapNamesByFolder(files).entrySet()) {
			String parent = entry.getKey().getName();
			String[] names = entry.getValue();
			
			for (String nameMatch : matchAll(names)) {
				String commonMatch = matchByFirstCommonWordSequence(nameMatch, parent);
				float similarity = commonMatch == null ? 0 : nameSimilarityMetric.getSimilarity(commonMatch, nameMatch);
				
				// prefer common match, but only if it's very similar to the original match
				seriesNames.add(similarity > 0.7 ? commonMatch : nameMatch);
			}
		}
		
		return seriesNames;
	}
	

	public Collection<String> matchAll(String[] names) {
		SeriesNameCollection seriesNames = new SeriesNameCollection();
		
		// allow matching of a small number of episodes, by setting threshold = length if length < 5
		int threshold = Math.min(names.length, 5);
		
		// match common word sequences (likely series names)
		SeriesNameCollection whitelist = new SeriesNameCollection();
		whitelist.addAll(deepMatchAll(names, threshold));
		
		// 1. use pattern matching
		seriesNames.addAll(flatMatchAll(names, Pattern.compile(join(whitelist, "|"), Pattern.CASE_INSENSITIVE), threshold, false));
		
		// 2. use common word sequences
		seriesNames.addAll(whitelist);
		
		return seriesNames;
	}
	

	/**
	 * Try to match and verify all series names using known season episode patterns.
	 * 
	 * @param names episode names
	 * @return series names that have been matched one or multiple times depending on the
	 *         threshold
	 */
	private Collection<String> flatMatchAll(String[] names, Pattern prefixPattern, int threshold, boolean strict) {
		ThresholdCollection<String> thresholdCollection = new ThresholdCollection<String>(threshold, String.CASE_INSENSITIVE_ORDER);
		
		for (String name : names) {
			// use normalized name
			name = normalize(name);
			
			Matcher prefix = prefixPattern.matcher(name);
			int sxePosition = seasonEpisodeMatcher.find(name, prefix.find() ? prefix.end() : 0);
			
			if (sxePosition > 0) {
				String hit = name.substring(0, sxePosition).trim();
				List<SxE> sxe = seasonEpisodeMatcher.match(name.substring(sxePosition));
				
				if (!strict && sxe.size() == 1 && sxe.get(0).season >= 0) {
					// bypass threshold if hit is likely to be genuine
					thresholdCollection.addDirect(hit);
				} else {
					// require multiple matches, if hit might be a false match
					thresholdCollection.add(hit);
				}
			}
		}
		
		return thresholdCollection;
	}
	

	/**
	 * Try to match all common word sequences in the given list.
	 * 
	 * @param names list of episode names
	 * @return all common word sequences that have been found
	 */
	private Collection<String> deepMatchAll(String[] names, int threshold) {
		// can't use common word sequence matching for less than 2 names
		if (names.length < 2 || names.length < threshold) {
			return emptySet();
		}
		
		String common = matchByFirstCommonWordSequence(names);
		
		if (common != null) {
			// common word sequence found
			return singleton(common);
		}
		
		// recursive divide and conquer
		List<String> results = new ArrayList<String>();
		
		// split list in two and try to match common word sequence on those
		results.addAll(deepMatchAll(Arrays.copyOfRange(names, 0, names.length / 2), threshold));
		results.addAll(deepMatchAll(Arrays.copyOfRange(names, names.length / 2, names.length), threshold));
		
		return results;
	}
	

	/**
	 * Try to match a series name from the given episode name using known season episode
	 * patterns.
	 * 
	 * @param name episode name
	 * @return a substring of the given name that ends before the first occurrence of a season
	 *         episode pattern, or null if there is no such pattern
	 */
	public String matchBySeasonEpisodePattern(String name) {
		int seasonEpisodePosition = seasonEpisodeMatcher.find(name, 0);
		
		if (seasonEpisodePosition > 0) {
			// series name ends at the first season episode pattern
			return normalize(name.substring(0, seasonEpisodePosition));
		}
		
		return null;
	}
	

	/**
	 * Try to match a series name from the first common word sequence.
	 * 
	 * @param names various episode names (at least two)
	 * @return a word sequence all episode names have in common, or null
	 * @throws IllegalArgumentException if less than 2 episode names are given
	 */
	public String matchByFirstCommonWordSequence(String... names) {
		if (names.length < 2) {
			throw new IllegalArgumentException("Can't match common sequence from less than two names");
		}
		
		String[] common = null;
		
		for (String name : names) {
			String[] words = normalize(name).split("\\s+");
			
			if (common == null) {
				// initialize common with current word array
				common = words;
			} else {
				// find common sequence
				common = firstCommonSequence(common, words, commonWordSequenceMaxStartIndex, String.CASE_INSENSITIVE_ORDER);
				
				if (common == null) {
					// no common sequence
					return null;
				}
			}
		}
		
		if (common == null)
			return null;
		
		return join(common, " ");
	}
	

	protected String normalize(String name) {
		// remove group names and checksums, any [...] or (...)
		name = name.replaceAll("\\([^\\(]*\\)", "");
		name = name.replaceAll("\\[[^\\[]*\\]", "");
		
		// remove special characters
		name = name.replaceAll("[\\p{Punct}\\p{Space}]+", " ");
		
		return name.trim();
	}
	

	protected <T> T[] firstCommonSequence(T[] seq1, T[] seq2, int maxStartIndex, Comparator<T> equalsComparator) {
		for (int i = 0; i < seq1.length && i <= maxStartIndex; i++) {
			for (int j = 0; j < seq2.length && j <= maxStartIndex; j++) {
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
					
					return Arrays.copyOfRange(seq1, i, i + len);
				}
			}
		}
		
		// no intersection at all
		return null;
	}
	

	private Map<File, String[]> mapNamesByFolder(File... files) {
		Map<File, List<File>> filesByFolder = new LinkedHashMap<File, List<File>>();
		
		for (File file : files) {
			File folder = file.getParentFile();
			
			List<File> list = filesByFolder.get(folder);
			
			if (list == null) {
				list = new ArrayList<File>();
				filesByFolder.put(folder, list);
			}
			
			list.add(file);
		}
		
		// convert folder->files map to folder->names map
		Map<File, String[]> namesByFolder = new LinkedHashMap<File, String[]>();
		
		for (Entry<File, List<File>> entry : filesByFolder.entrySet()) {
			namesByFolder.put(entry.getKey(), names(entry.getValue()));
		}
		
		return namesByFolder;
	}
	

	protected String[] names(Collection<File> files) {
		String[] names = new String[files.size()];
		
		int i = 0;
		
		// fill array
		for (File file : files) {
			names[i++] = FileUtilities.getName(file);
		}
		
		return names;
	}
	

	protected static class SeriesNameCollection extends AbstractCollection<String> {
		
		private final Map<String, String> data = new LinkedHashMap<String, String>();
		

		@Override
		public boolean add(String value) {
			String current = data.get(key(value));
			
			// prefer strings with similar upper/lower case ration (e.g. prefer Roswell over roswell) 
			if (current == null || firstCharacterCaseBalance(current) < firstCharacterCaseBalance(value)) {
				data.put(key(value), value);
				return true;
			}
			
			return false;
		}
		

		protected String key(Object value) {
			return value.toString().toLowerCase();
		}
		

		protected float firstCharacterCaseBalance(String s) {
			int upper = 0;
			int lower = 0;
			
			Scanner scanner = new Scanner(s); // Scanner uses a white space delimiter by default
			
			while (scanner.hasNext()) {
				char c = scanner.next().charAt(0);
				
				if (Character.isLowerCase(c))
					lower++;
				else if (Character.isUpperCase(c))
					upper++;
			}
			
			// give upper case characters a slight boost over lower case characters
			return (lower + (upper * 1.01f)) / Math.abs(lower - upper);
		}
		

		@Override
		public boolean contains(Object value) {
			return data.containsKey(key(value));
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
		public boolean add(E value) {
			Collection<E> buffer = limbo.get(value);
			
			if (buffer == null) {
				// initialize buffer
				buffer = new ArrayList<E>(threshold);
				limbo.put(value, buffer);
			}
			
			if (buffer == heaven) {
				// threshold reached
				heaven.add(value);
				return true;
			}
			
			// add element to buffer
			buffer.add(value);
			
			// check if threshold has been reached
			if (buffer.size() >= threshold) {
				heaven.addAll(buffer);
				
				// replace buffer with heaven
				limbo.put(value, heaven);
				return true;
			}
			
			return false;
		};
		

		public boolean addDirect(E element) {
			return heaven.add(element);
		}
		

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
