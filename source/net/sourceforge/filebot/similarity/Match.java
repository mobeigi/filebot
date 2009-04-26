
package net.sourceforge.filebot.similarity;


public class Match<Value, Candidate> {
	
	private final Value value;
	private final Candidate candidate;
	
	
	public Match(Value value, Candidate candidate) {
		this.value = value;
		this.candidate = candidate;
	}
	

	public Value getValue() {
		return value;
	}
	

	public Candidate getCandidate() {
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
	public boolean equals(Object obj) {
		if (obj instanceof Match) {
			Match<?, ?> other = (Match<?, ?>) obj;
			return value == other.value && candidate == other.candidate;
		}
		
		return false;
	}
	

	@Override
	public int hashCode() {
		return (value == null ? 0 : value.hashCode()) ^ (candidate == null ? 0 : candidate.hashCode());
	}
	

	@Override
	public String toString() {
		return String.format("[%s, %s]", value, candidate);
	}
	
}
