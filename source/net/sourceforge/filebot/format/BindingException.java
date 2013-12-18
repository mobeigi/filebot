package net.sourceforge.filebot.format;

public class BindingException extends RuntimeException {

	public BindingException(String message, Throwable cause) {
		super(message, cause);
	}

	public BindingException(String binding, String innerMessage) {
		this(binding, innerMessage, null);
	}

	public BindingException(String binding, String innerMessage, Throwable cause) {
		this(String.format("BindingException: \"%s\": %s", binding, innerMessage), cause);
	}

}
