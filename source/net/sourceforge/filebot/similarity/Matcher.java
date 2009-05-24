
package net.sourceforge.filebot.similarity;


import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;


public class Matcher<V, C> {
	
	private final List<V> values;
	private final List<C> candidates;
	
	private final SimilarityMetric[] metrics;
	
	private final DisjointMatchCollection<V, C> disjointMatchCollection;
	
	
	public Matcher(Collection<? extends V> values, Collection<? extends C> candidates, Collection<? extends SimilarityMetric> metrics) {
		this.values = new LinkedList<V>(values);
		this.candidates = new LinkedList<C>(candidates);
		
		this.metrics = metrics.toArray(new SimilarityMetric[0]);
		
		this.disjointMatchCollection = new DisjointMatchCollection<V, C>();
	}
	

	public synchronized List<Match<V, C>> match() throws InterruptedException {
		
		// list of all combinations of values and candidates
		List<Match<V, C>> possibleMatches = new ArrayList<Match<V, C>>(values.size() * candidates.size());
		
		// populate with all possible matches
		for (V value : values) {
			for (C candidate : candidates) {
				possibleMatches.add(new Match<V, C>(value, candidate));
			}
		}
		
		// match recursively
		deepMatch(possibleMatches, 0);
		
		// restore order according to the given values
		List<Match<V, C>> result = new ArrayList<Match<V, C>>();
		
		for (V value : values) {
			Match<V, C> match = disjointMatchCollection.getByValue(value);
			
			if (match != null) {
				result.add(match);
			}
		}
		
		// remove matched objects
		for (Match<V, C> match : result) {
			values.remove(match.getValue());
			candidates.remove(match.getCandidate());
		}
		
		// clear collected matches
		disjointMatchCollection.clear();
		
		return result;
	}
	

	public synchronized List<V> remainingValues() {
		return Collections.unmodifiableList(values);
	}
	

	public synchronized List<C> remainingCandidates() {
		return Collections.unmodifiableList(candidates);
	}
	

	protected void deepMatch(Collection<Match<V, C>> possibleMatches, int level) throws InterruptedException {
		if (level >= metrics.length || possibleMatches.isEmpty()) {
			// no further refinement possible
			disjointMatchCollection.addAll(possibleMatches);
			return;
		}
		
		for (List<Match<V, C>> matchesWithEqualSimilarity : mapBySimilarity(possibleMatches, metrics[level]).values()) {
			// some matches may already be unique
			List<Match<V, C>> disjointMatches = disjointMatches(matchesWithEqualSimilarity);
			
			if (!disjointMatches.isEmpty()) {
				// collect disjoint matches
				disjointMatchCollection.addAll(disjointMatches);
				
				// no need for further matching
				matchesWithEqualSimilarity.removeAll(disjointMatches);
			}
			
			// remove invalid matches
			removeCollected(matchesWithEqualSimilarity);
			
			// matches may be ambiguous, more refined matching required
			deepMatch(matchesWithEqualSimilarity, level + 1);
		}
	}
	

	protected void removeCollected(Collection<Match<V, C>> matches) {
		for (Iterator<Match<V, C>> iterator = matches.iterator(); iterator.hasNext();) {
			if (!disjointMatchCollection.disjoint(iterator.next()))
				iterator.remove();
		}
	}
	

	protected SortedMap<Float, List<Match<V, C>>> mapBySimilarity(Collection<Match<V, C>> possibleMatches, SimilarityMetric metric) throws InterruptedException {
		// map sorted by similarity descending
		SortedMap<Float, List<Match<V, C>>> similarityMap = new TreeMap<Float, List<Match<V, C>>>(Collections.reverseOrder());
		
		// use metric on all matches
		for (Match<V, C> possibleMatch : possibleMatches) {
			float similarity = metric.getSimilarity(possibleMatch.getValue(), possibleMatch.getCandidate());
			
			List<Match<V, C>> list = similarityMap.get(similarity);
			
			if (list == null) {
				list = new ArrayList<Match<V, C>>();
				similarityMap.put(similarity, list);
			}
			
			list.add(possibleMatch);
			
			// unwind this thread if we have been interrupted
			if (Thread.interrupted()) {
				throw new InterruptedException();
			}
		}
		
		return similarityMap;
	}
	

	protected List<Match<V, C>> disjointMatches(Collection<Match<V, C>> collection) {
		List<Match<V, C>> disjointMatches = new ArrayList<Match<V, C>>();
		
		for (Match<V, C> m1 : collection) {
			boolean disjoint = true;
			
			for (Match<V, C> m2 : collection) {
				// ignore same element
				if (m1 != m2 && !m1.disjoint(m2)) {
					disjoint = false;
					break;
				}
			}
			
			if (disjoint) {
				disjointMatches.add(m1);
			}
		}
		
		return disjointMatches;
	}
	
	
	protected static class DisjointMatchCollection<V, C> extends AbstractList<Match<V, C>> {
		
		private final List<Match<V, C>> matches = new ArrayList<Match<V, C>>();
		
		private final Map<V, Match<V, C>> values = new IdentityHashMap<V, Match<V, C>>();
		private final Map<C, Match<V, C>> candidates = new IdentityHashMap<C, Match<V, C>>();
		
		
		@Override
		public boolean add(Match<V, C> match) {
			if (disjoint(match)) {
				values.put(match.getValue(), match);
				candidates.put(match.getCandidate(), match);
				
				return matches.add(match);
			}
			
			return false;
		}
		

		public boolean disjoint(Match<V, C> match) {
			return !values.containsKey(match.getValue()) && !candidates.containsKey(match.getCandidate());
		}
		

		public Match<V, C> getByValue(V value) {
			return values.get(value);
		}
		

		public Match<V, C> getByCandidate(C candidate) {
			return candidates.get(candidate);
		}
		

		@Override
		public Match<V, C> get(int index) {
			return matches.get(index);
		}
		

		@Override
		public int size() {
			return matches.size();
		}
		

		@Override
		public void clear() {
			matches.clear();
			values.clear();
			candidates.clear();
		}
		
	}
	
}
