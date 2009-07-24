
package net.sourceforge.tuned;


import java.util.Iterator;


public final class StringUtilities {
	
	public static String join(Object[] values, CharSequence delimiter) {
		StringBuilder sb = new StringBuilder();
		
		for (int i = 0; i < values.length; i++) {
			sb.append(values[i]);
			
			if (i < values.length - 1) {
				sb.append(delimiter);
			}
		}
		
		return sb.toString();
	}
	

	public static String join(Iterable<?> values, CharSequence delimiter) {
		StringBuilder sb = new StringBuilder();
		
		for (Iterator<?> iterator = values.iterator(); iterator.hasNext();) {
			sb.append(iterator.next());
			
			if (iterator.hasNext()) {
				sb.append(delimiter);
			}
		}
		
		return sb.toString();
	}
	

	public static boolean isNullOrEmpty(String value) {
		return value == null || value.isEmpty();
	}
	

	/**
	 * Dummy constructor to prevent instantiation.
	 */
	private StringUtilities() {
		throw new UnsupportedOperationException();
	}
	
}
