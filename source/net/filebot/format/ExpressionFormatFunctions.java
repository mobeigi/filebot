package net.filebot.format;

import static java.util.stream.Collectors.*;
import static net.filebot.util.FileUtilities.*;
import static net.filebot.util.RegularExpressions.*;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import groovy.lang.Closure;

/**
 * Global functions available in the {@link ExpressionFormat}
 */
public class ExpressionFormatFunctions {

	/**
	 * General helpers and utilities
	 */
	public static Object call(Object object) {
		if (object instanceof Closure<?>) {
			try {
				return ((Closure<?>) object).call();
			} catch (Exception e) {
				return null;
			}
		}
		if (object instanceof CharSequence && object.toString().isEmpty()) {
			return null;
		}
		return object;
	}

	public static Object any(Object c1, Object c2, Object... cN) {
		return stream(c1, c2, cN).findFirst().orElse(null);
	}

	public static List<Object> allOf(Object c1, Object c2, Object... cN) {
		return stream(c1, c2, cN).collect(toList());
	}

	public static String concat(Object c1, Object c2, Object... cN) {
		return stream(c1, c2, cN).map(Objects::toString).collect(joining());
	}

	protected static Stream<Object> stream(Object c1, Object c2, Object... cN) {
		return Stream.concat(Stream.of(c1, c2), Stream.of(cN)).map(ExpressionFormatFunctions::call).filter(Objects::nonNull);
	}

	public static Map<String, String> csv(String path) throws IOException {
		Map<String, String> map = new LinkedHashMap<String, String>();
		streamLines(new File(path)).forEach(line -> {
			for (Pattern delim : new Pattern[] { TAB, SEMICOLON }) {
				String[] field = delim.split(line, 2);
				if (field.length >= 2) {
					map.put(field[0].trim(), field[1].trim());
					break;
				}
			}
		});
		return map;
	}

	public static List<String> readLines(String path) throws IOException {
		return streamLines(new File(path)).collect(toList());
	}

}
