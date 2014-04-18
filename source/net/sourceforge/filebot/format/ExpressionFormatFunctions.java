package net.sourceforge.filebot.format;

import groovy.lang.Closure;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Global functions available in the {@link ExpressionFormat}
 */
public class ExpressionFormatFunctions {

	/**
	 * General helpers and utilities
	 */
	public static Object c(Closure<?> c) {
		try {
			return c.call();
		} catch (Exception e) {
			return null;
		}
	}

	public static Object any(Object a0, Object a1, Object... args) {
		for (Object it : new Object[] { a0, a1 }) {
			try {
				Object result = callIfCallable(it);
				if (result != null) {
					return result;
				}
			} catch (Exception e) {
				// ignore
			}
		}

		for (Object it : args) {
			try {
				Object result = callIfCallable(it);
				if (result != null) {
					return result;
				}
			} catch (Exception e) {
				// ignore
			}
		}

		return null;
	}

	public static List<Object> allOf(Object a0, Object a1, Object... args) {
		List<Object> values = new ArrayList<Object>();

		for (Object it : new Object[] { a0, a1 }) {
			try {
				Object result = callIfCallable(it);
				if (result != null) {
					values.add(result);
				}
			} catch (Exception e) {
				// ignore
			}
		}

		for (Object it : args) {
			try {
				Object result = callIfCallable(it);
				if (result != null) {
					values.add(result);
				}
			} catch (Exception e) {
				// ignore
			}
		}

		return values;
	}

	private static Object callIfCallable(Object obj) throws Exception {
		if (obj instanceof Closure<?>) {
			return ((Closure<?>) obj).call();
		}
		return obj;
	}

	public Map<String, String> csv(String path) throws IOException {
		Map<String, String> map = new LinkedHashMap<String, String>();
		for (String line : Files.readAllLines(Paths.get(path), Charset.forName("UTF-8"))) {
			String[] field = line.split(";", 2);
			map.put(field[0], field[1]);
		}
		return map;
	}

}
