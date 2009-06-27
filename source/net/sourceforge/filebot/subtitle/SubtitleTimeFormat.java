
package net.sourceforge.filebot.subtitle;


import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Scanner;
import java.util.TimeZone;


class SubtitleTimeFormat extends DateFormat {
	
	public SubtitleTimeFormat() {
		// calendar without any kind of special handling for time zone and daylight saving time
		calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT);
	}
	

	@Override
	public StringBuffer format(Date date, StringBuffer sb, FieldPosition pos) {
		// e.g. 1:42:52.42
		calendar.setTime(date);
		
		sb.append(String.format("%02d", calendar.get(Calendar.HOUR_OF_DAY)));
		sb.append(':').append(String.format("%02d", calendar.get(Calendar.MINUTE)));
		sb.append(':').append(String.format("%02d", calendar.get(Calendar.SECOND)));
		
		String millis = String.format("%03d", calendar.get(Calendar.MILLISECOND));
		sb.append('.').append(millis.substring(0, 2));
		
		return sb;
	}
	

	@Override
	public Date parse(String source, ParsePosition pos) {
		Scanner scanner = new Scanner(source).useDelimiter(":|\\.");
		
		// reset state
		calendar.clear();
		
		// handle hours:minutes:seconds
		calendar.set(Calendar.HOUR_OF_DAY, scanner.nextInt());
		calendar.set(Calendar.MINUTE, scanner.nextInt());
		calendar.set(Calendar.SECOND, scanner.nextInt());
		
		// handle hundredth seconds
		calendar.set(Calendar.MILLISECOND, scanner.nextInt() * 10);
		
		// update position
		pos.setIndex(scanner.match().end());
		
		return calendar.getTime();
	}
}
