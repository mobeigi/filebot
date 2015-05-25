package net.filebot.util;

import static java.util.Arrays.*;

import java.util.Arrays;
import java.util.Comparator;

public final class StringUtilities {

	public static String asString(Object object) {
		return object == null ? null : object.toString();
	}

	public static boolean isEmpty(Object object) {
		return object == null || object.toString().length() == 0;
	}

	public static String join(Iterable<?> values, CharSequence delimiter) {
		return join(values, delimiter, "", "");
	}

	public static String join(CharSequence delimiter, Object... values) {
		return join(asList(values), delimiter, "", "");
	}

	public static String join(Object[] values, CharSequence delimiter) {
		return join(asList(values), delimiter, "", "");
	}

	public static String joinSorted(Object[] values, CharSequence delimiter, Comparator<Object> sort, CharSequence start, CharSequence end) {
		return join(Arrays.stream(values).sorted(sort)::iterator, delimiter, start, end);
	}

	public static String join(Iterable<?> values, CharSequence delimiter, CharSequence start, CharSequence end) {
		StringBuilder sb = new StringBuilder().append(start);

		for (Object value : values) {
			if (!isEmpty(value)) {
				if (sb.length() > start.length()) {
					sb.append(delimiter);
				}
				sb.append(value);
			}
		}

		return sb.append(end).toString();
	}

	/**
	 * Dummy constructor to prevent instantiation.
	 */
	private StringUtilities() {
		throw new UnsupportedOperationException();
	}

}
