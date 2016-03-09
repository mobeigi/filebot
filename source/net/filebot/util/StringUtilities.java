package net.filebot.util;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static java.util.stream.Collectors.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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
			try {
				numbers.add(new Integer(matcher.group()));
			} catch (NumberFormatException e) {
				// ignore
			}
		}
		return numbers;
	}

	public static Integer matchInteger(CharSequence s) {
		if (s == null || s.length() == 0) {
			return null;
		}

		Matcher matcher = DIGIT.matcher(s);
		if (matcher.find()) {
			try {
				return new Integer(matcher.group());
			} catch (NumberFormatException e) {
				// ignore
			}
		}
		return null;
	}

	public static String asString(Object object) {
		return object == null ? null : object.toString();
	}

	public static String asNonEmptyString(Object object) {
		if (object != null) {
			String string = object.toString();
			if (string.length() > 0) {
				return string;
			}
		}
		return null;
	}

	public static boolean isEmpty(Object object) {
		return object == null || object.toString().length() == 0;
	}

	public static boolean nonEmpty(Object object) {
		return object != null && object.toString().length() > 0;
	}

	public static String join(Collection<?> values, CharSequence delimiter) {
		return join(values.stream(), delimiter, "", "");
	}

	public static String join(Object[] values, CharSequence delimiter) {
		return join(stream(values), delimiter, "", "");
	}

	public static String join(Stream<?> values, CharSequence delimiter, CharSequence prefix, CharSequence suffix) {
		return values.map(StringUtilities::asNonEmptyString).filter(Objects::nonNull).collect(joining(delimiter, prefix, suffix));
	}

	/**
	 * Dummy constructor to prevent instantiation.
	 */
	private StringUtilities() {
		throw new UnsupportedOperationException();
	}

}
