
package net.sourceforge.filebot.web;


import static java.util.Calendar.*;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;


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
	

	@Override
	public String toString() {
		return String.format("%04d-%02d-%02d", year, month, day);
	}
	

	public String format(String pattern) {
		return new SimpleDateFormat(pattern).format(new GregorianCalendar(year, month, day).getTime());
	}
	

	public static Date parse(String string, String pattern) {
		SimpleDateFormat formatter = new SimpleDateFormat(pattern, Locale.ROOT);
		formatter.setLenient(false); // enable strict mode (e.g. fail on invalid dates like 0000-00-00)
		
		try {
			Calendar date = new GregorianCalendar(Locale.ROOT);
			date.setTime(formatter.parse(string));
			return new Date(date.get(YEAR), date.get(MONTH) + 1, date.get(DAY_OF_MONTH));
		} catch (ParseException e) {
			// no result if date is invalid
			Logger.getLogger(Date.class.getName()).log(Level.WARNING, e.getMessage());
			return null;
		}
	}
	
}
