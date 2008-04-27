
package net.sourceforge.filebot.ui.panel.rename.match;


import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.sourceforge.filebot.ui.panel.rename.entry.ListEntry;
import net.sourceforge.filebot.ui.panel.rename.similarity.SimilarityMetric;


public class Matcher implements Iterator<Match> {
	
	private final LinkedList<ListEntry> primaryList;
	private final LinkedList<ListEntry> secondaryList;
	private final SimilarityMetric similarityMetric;
	
	
	public Matcher(List<? extends ListEntry> primaryList, List<? extends ListEntry> secondaryList, SimilarityMetric similarityMetric) {
		this.primaryList = new LinkedList<ListEntry>(primaryList);
		this.secondaryList = new LinkedList<ListEntry>(secondaryList);
		this.similarityMetric = similarityMetric;
	}
	

	@Override
	public boolean hasNext() {
		return remainingMatches() > 0;
	}
	

	@Override
	public Match next() {
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
	

	public int remainingMatches() {
		return Math.min(primaryList.size(), secondaryList.size());
	}
	

	public List<ListEntry> getPrimaryList() {
		return Collections.unmodifiableList(primaryList);
	}
	

	public List<ListEntry> getSecondaryList() {
		return Collections.unmodifiableList(secondaryList);
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
