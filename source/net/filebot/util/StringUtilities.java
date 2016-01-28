package net.filebot.util;

import static java.util.Arrays.*;
import static java.util.Collections.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StringUtilities {

	public static final Pattern SPACE = Pattern.compile("\\s+");
	public static final Pattern DIGIT = Pattern.compile("\\d+");
	public static final Pattern NON_DIGIT = Pattern.compile("\\D+");
	public static final Pattern PIPE = Pattern.compile("|", Pattern.LITERAL);

	public static List<Integer> matchIntegers(CharSequence s) {
		if (s == null || s.length() == 0) {
			return emptyList();
		}

		List<Integer> numbers = new ArrayList<Integer>();
		Matcher matcher = DIGIT.matcher(s);
		while (matcher.find()) {
			numbers.add(new Integer(matcher.group()));
		}
		return numbers;
	}

	public static Integer matchInteger(CharSequence s) {
		if (s == null || s.length() == 0) {
			return null;
		}

		Matcher matcher = DIGIT.matcher(s);
		if (matcher.find()) {
			return new Integer(matcher.group());
		}
		return null;
	}

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
		return join(stream(values).sorted(sort)::iterator, delimiter, start, end);
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
