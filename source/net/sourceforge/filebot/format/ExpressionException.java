
package net.sourceforge.filebot.format;


import javax.script.ScriptException;


public class ExpressionException extends ScriptException {
	
	private final String message;
	
	
	public ExpressionException(String message, Exception cause) {
		super(cause);
		
		// can't set message via super constructor
		this.message = message;
	}
	

	public ExpressionException(Exception e) {
		this(e.getMessage(), e);
	}
	

	@Override
	public String getMessage() {
		return message;
	}
	
}
