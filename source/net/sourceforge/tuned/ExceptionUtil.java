
package net.sourceforge.tuned;


public final class ExceptionUtil {
	
	public static Throwable getRootCause(Throwable t) {
		while (t.getCause() != null) {
			t = t.getCause();
		}
		
		return t;
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
	private ExceptionUtil() {
		throw new UnsupportedOperationException();
	}
	
}
