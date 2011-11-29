
package net.sourceforge.tuned;


import static java.util.Arrays.*;

import java.util.Iterator;


public final class StringUtilities {
	
	public static boolean isEmptyValue(Object object) {
		return object == null || object.toString().length() == 0;
	}
	

	public static String joinBy(CharSequence delimiter, Object... values) {
		return join(asList(values), delimiter);
	}
	

	public static String join(Object[] values, CharSequence delimiter) {
		return join(asList(values), delimiter);
	}
	

	public static String join(Iterable<?> values, CharSequence delimiter) {
		StringBuilder sb = new StringBuilder();
		
		for (Iterator<?> iterator = values.iterator(); iterator.hasNext();) {
			Object value = iterator.next();
			if (!isEmptyValue(value)) {
				if (sb.length() > 0) {
					sb.append(delimiter);
				}
				
				sb.append(value);
			}
		}
		
		return sb.toString();
	}
	

	/**
	 * Dummy constructor to prevent instantiation.
	 */
	private StringUtilities() {
		throw new UnsupportedOperationException();
	}
	
}
