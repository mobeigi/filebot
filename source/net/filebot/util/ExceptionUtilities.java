package net.filebot.util;

public final class ExceptionUtilities {

	public static Throwable getRootCause(Throwable t) {
		while (t.getCause() != null) {
			t = t.getCause();
		}
		return t;
	}

	public static <T extends Throwable> T findCause(Throwable t, Class<T> type) {
		while (t != null) {
			if (type.isInstance(t)) {
				return type.cast(t);
			}
			t = t.getCause();
		}
		return null;
	}

	public static String getRootCauseMessage(Throwable t) {
		return getMessage(getRootCause(t));
	}

	public static String getMessage(Throwable t) {
		String m = t.getMessage();
		if (m == null || m.isEmpty()) {
			m = t.toString();
		}
		return m;
	}

	private ExceptionUtilities() {
		throw new UnsupportedOperationException();
	}

}
