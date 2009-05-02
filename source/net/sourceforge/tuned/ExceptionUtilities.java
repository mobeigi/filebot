
package net.sourceforge.tuned;


public final class ExceptionUtilities {
	
	public static Throwable getRootCause(Throwable t) {
		while (t.getCause() != null) {
			t = t.getCause();
		}
		
		return t;
	}
	

	@SuppressWarnings("unchecked")
	public static <T extends Throwable> T findCause(Throwable t, Class<T> type) {
		while (t != null) {
			if (type.isInstance(t))
				return (T) t;
			
			t = t.getCause();
		}
		
		return null;
	}
	

	public static String getRootCauseMessage(Throwable t) {
		return getMessage(getRootCause(t));
	}
	

	public static String getMessage(Throwable t) {
		String message = t.getMessage();
		
		if (message == null || message.isEmpty()) {
			message = t.toString();
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
