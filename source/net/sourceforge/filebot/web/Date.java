
package net.sourceforge.filebot.web;


import static java.util.Calendar.*;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;


public class Date implements Serializable {
	
	private int year;
	private int month;
	private int day;
	
	
	protected Date() {
		// used by serializer
	}
	
	
	public Date(int year, int month, int day) {
		this.year = year;
		this.month = month;
		this.day = day;
	}
	
	
	public int getYear() {
		return year;
	}
	
	
	public int getMonth() {
		return month;
	}
	
	
	public int getDay() {
		return day;
	}
	
	
	public long getTimeStamp() {
		return new GregorianCalendar(year, month - 1, day).getTimeInMillis(); // Month value is 0-based, e.g. 0 for January
	}
	
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Date) {
			Date other = (Date) obj;
			return year == other.year && month == other.month && day == other.day;
		}
		
		return super.equals(obj);
	}
	
	
	@Override
	public int hashCode() {
		return Arrays.hashCode(new Object[] { year, month, day });
	}
	
	
	@Override
	public String toString() {
		return String.format("%04d-%02d-%02d", year, month, day);
	}
	
	
	public String format(String pattern) {
		return format(pattern, Locale.ROOT);
	}
	
	
	public String format(String pattern, Locale locale) {
		return new SimpleDateFormat(pattern, locale).format(new GregorianCalendar(year, month - 1, day).getTime()); // Calendar months start at 0
	}
	
	
	public static Date parse(String string, String pattern) {
		if (string == null || string.isEmpty())
			return null;
		
		SimpleDateFormat formatter = new SimpleDateFormat(pattern, Locale.ROOT);
		formatter.setLenient(false); // enable strict mode (e.g. fail on invalid dates like 0000-00-00)
		
		try {
			Calendar date = new GregorianCalendar(Locale.ROOT);
			date.setTime(formatter.parse(string));
			return new Date(date.get(YEAR), date.get(MONTH) + 1, date.get(DAY_OF_MONTH)); // Calendar months start at 0
		} catch (ParseException e) {
			// no result if date is invalid
			// Logger.getLogger(Date.class.getName()).log(Level.WARNING, e.getMessage());
			return null;
		}
	}
	
}
