package net.sourceforge.filebot.format;

import groovy.lang.Closure;

import java.util.ArrayList;
import java.util.List;

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

	public static Object any(Closure<?>... closures) {
		for (Closure<?> it : closures) {
			try {
				Object result = it.call();
				if (result != null) {
					return result;
				}
			} catch (Exception e) {
				// ignore
			}
		}
		return null;
	}

	public static List<Object> allOf(Closure<?>... closures) {
		List<Object> values = new ArrayList<Object>();

		for (Closure<?> it : closures) {
			try {
				Object result = it.call();
				if (result != null) {
					values.add(result);
				}
			} catch (Exception e) {
				// ignore
			}
		}

		return values;
	}

}
