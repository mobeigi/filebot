
package net.sourceforge.filebot.similarity;


import java.util.Arrays;


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
		return Arrays.hashCode(new Object[] { value, candidate });
	}
	

	@Override
	public String toString() {
		return String.format("[%s, %s]", value, candidate);
	}
	
}
