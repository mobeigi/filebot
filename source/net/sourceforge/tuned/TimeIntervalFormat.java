
package net.sourceforge.tuned;


import java.util.TreeMap;


public class TimeIntervalFormat {
	
	private static TreeMap<Long, String> unitMap = new TreeMap<Long, String>();
	
	static {
		unitMap.put(1L, "ms");
		unitMap.put(1000L, "s");
		unitMap.put(60 * 1000L, "m");
		unitMap.put(60 * 60 * 1000L, "h");
	}
	
	
	public static String format(long millis, boolean zerounits) {
		boolean negativ = false;
		
		if (millis < 0) {
			millis = Math.abs(millis);
			negativ = true;
		}
		
		StringBuilder sb = new StringBuilder();
		for (long unitBaseTime : unitMap.descendingKeySet()) {
			int quotient = (int) (millis / unitBaseTime);
			
			boolean isLastKey = (unitBaseTime == unitMap.firstKey());
			
			if (zerounits || (quotient != 0) || isLastKey) {
				sb.append(quotient + unitMap.get(unitBaseTime));
				
				if (!isLastKey)
					;
				sb.append(" ");
			}
			
			millis -= quotient * unitBaseTime;
		}
		
		if (negativ)
			sb.insert(0, "-");
		
		return sb.toString();
	}
	
}
