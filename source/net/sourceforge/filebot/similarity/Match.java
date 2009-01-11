
package net.sourceforge.filebot.similarity;


public class Match<V, C> {
	
	private final V value;
	private final C candidate;
	
	
	public Match(V value, C candidate) {
		this.value = value;
		this.candidate = candidate;
	}
	

	public V getValue() {
		return value;
	}
	

	public C getCandidate() {
		return candidate;
	}
	

	/**
	 * Check if the given match has the same value or the same candidate. This method uses an
	 * <b>identity equality test</b>.
	 * 
	 * @param match a match
	 * @return Returns <code>true</code> if the specified match has no value common.
	 */
	public boolean disjoint(Match<?, ?> match) {
		return (value != match.value && candidate != match.candidate);
	}
	

	@Override
	public String toString() {
		return String.format("[%s, %s]", value, candidate);
	}
	
}
