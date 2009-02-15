
package net.sourceforge.tuned;


public final class ExceptionUtilities {
	
	public static Throwable getRootCause(Throwable t) {
		while (t.getCause() != null) {
			t = t.getCause();
		}
		
		return t;
	}
	

	public static String getRootCauseMessage(Throwable t) {
		return getMessage(getRootCause(t));
	}
	

	public static String getMessage(Throwable t) {
		String message = t.getMessage();
		
		if (message == null || message.isEmpty()) {
			message = t.toString().replaceAll(t.getClass().getName(), t.getClass().getSimpleName());
		}
		
		return message;
	}
	

	public static RuntimeException asRuntimeException(Throwable t) {
		if (t instanceof RuntimeException) {
			return (RuntimeException) t;
		}
		
		return new RuntimeException(t);
	}
	

	/**
	 * Dummy constructor to prevent instantiation.
	 */
	private ExceptionUtilities() {
		throw new UnsupportedOperationException();
	}
	
}
