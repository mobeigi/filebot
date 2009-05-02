
package net.sourceforge.filebot.hash;


public class IllegalSyntaxException extends RuntimeException {
	
	public IllegalSyntaxException(int lineNumber, String line) {
		this(String.format("Illegal syntax in line %d: %s", lineNumber, line));
	}
	

	public IllegalSyntaxException(String message) {
		super(message);
	}
	
}
