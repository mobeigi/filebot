
package net.sourceforge.tuned;


public final class ExceptionUtilities {
	
	public static Throwable getRootCause(Throwable t) {
		while (t.getCause() != null) {
			t = t.getCause();
		}
		
		return t;
	}
	

	public static <T extends Throwable> T findCause(Throwable t, Class<T> type) {
		while (t != null) {
			if (type.isInstance(t))
				return type.cast(t);
			
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
	

	public static <T extends Throwable> T wrap(Throwable t, Class<T> type) {
		if (type.isInstance(t)) {
			return type.cast(t);
		}
		
		try {
			return type.getConstructor(Throwable.class).newInstance(t);
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}
	

	public static RuntimeException asRuntimeException(Throwable t) {
		return wrap(t, RuntimeException.class);
	}
	

	/**
	 * Dummy constructor to prevent instantiation.
	 */
	private ExceptionUtilities() {
		throw new UnsupportedOperationException();
	}
	
}
