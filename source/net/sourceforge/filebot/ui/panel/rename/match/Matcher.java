
package net.sourceforge.filebot.ui.panel.rename.match;


import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.sourceforge.filebot.ui.panel.rename.entry.ListEntry;
import net.sourceforge.filebot.ui.panel.rename.similarity.SimilarityMetric;


public class Matcher implements Iterable<Match>, Iterator<Match> {
	
	private final LinkedList<ListEntry> primaryList;
	private final LinkedList<ListEntry> secondaryList;
	private final SimilarityMetric similarityMetric;
	
	
	public Matcher(List<? extends ListEntry> primaryList, List<? extends ListEntry> secondaryList, SimilarityMetric similarityMetric) {
		this.primaryList = new LinkedList<ListEntry>(primaryList);
		this.secondaryList = new LinkedList<ListEntry>(secondaryList);
		this.similarityMetric = similarityMetric;
	}
	

	@Override
	public Iterator<Match> iterator() {
		return this;
	}
	

	@Override
	public boolean hasNext() {
		return !primaryList.isEmpty() || !secondaryList.isEmpty();
	}
	

	@Override
	public Match next() {
		if (primaryList.isEmpty()) {
			return new Match(null, secondaryList.removeFirst());
		}
		
		ListEntry primaryEntry = primaryList.removeFirst();
		
		float maxSimilarity = -1;
		ListEntry mostSimilarSecondaryEntry = null;
		
		for (ListEntry secondaryEntry : secondaryList) {
			float similarity = similarityMetric.getSimilarity(primaryEntry, secondaryEntry);
			
			if (similarity > maxSimilarity) {
				maxSimilarity = similarity;
				mostSimilarSecondaryEntry = secondaryEntry;
			}
		}
		
		if (mostSimilarSecondaryEntry != null) {
			secondaryList.remove(mostSimilarSecondaryEntry);
		}
		
		return new Match(primaryEntry, mostSimilarSecondaryEntry);
	}
	

	public ListEntry getFirstPrimaryEntry() {
		if (primaryList.isEmpty())
			return null;
		
		return primaryList.getFirst();
	}
	

	public ListEntry getFirstSecondaryEntry() {
		if (secondaryList.isEmpty())
			return null;
		
		return secondaryList.getFirst();
	}
	

	public int remaining() {
		return Math.max(primaryList.size(), secondaryList.size());
	}
	

	/**
	 * The remove operation is not supported by this implementation of <code>Iterator</code>.
	 * 
	 * @throws UnsupportedOperationException if this method is invoked.
	 * @see java.util.Iterator
	 */
	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
