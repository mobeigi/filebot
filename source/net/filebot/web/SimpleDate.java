package net.filebot.web;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleDate implements Serializable, Comparable<Object> {

	private int year;
	private int month;
	private int day;

	protected SimpleDate() {
		// used by serializer
	}

	public SimpleDate(int year, int month, int day) {
		this.year = year;
		this.month = month;
		this.day = day;
	}

	public SimpleDate(LocalDate date) {
		this(date.getYear(), date.getMonthValue(), date.getDayOfMonth());
	}

	public SimpleDate(long t) {
		this(LocalDateTime.ofInstant(Instant.ofEpochMilli(t), ZoneId.systemDefault()).toLocalDate());
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
		return this.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SimpleDate) {
			SimpleDate other = (SimpleDate) obj;
			return year == other.year && month == other.month && day == other.day;
		} else if (obj instanceof CharSequence) {
			return this.toString().equals(obj.toString());
		}

		return super.equals(obj);
	}

	@Override
	public int compareTo(Object other) {
		if (other instanceof SimpleDate) {
			return compareTo((SimpleDate) other);
		} else if (other instanceof CharSequence) {
			SimpleDate otherDate = parse(other.toString());
			if (otherDate != null) {
				return compareTo(otherDate);
			}
		}

		throw new IllegalArgumentException(String.valueOf(other));
	}

	public int compareTo(SimpleDate other) {
		return Long.compare(this.getTimeStamp(), other.getTimeStamp());
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(new Object[] { year, month, day });
	}

	@Override
	public SimpleDate clone() {
		return new SimpleDate(year, month, day);
	}

	public String format(String pattern) {
		return DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH).format(this.toLocalDate());
	}

	public LocalDate toLocalDate() {
		return LocalDate.of(year, month, day);
	}

	@Override
	public String toString() {
		return String.format("%04d-%02d-%02d", year, month, day);
	}

	public static SimpleDate parse(String date) {
		if (date != null && date.length() > 0) {
			Matcher m = DATE_FORMAT.matcher(date);
			if (m.matches()) {
				return new SimpleDate(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
			}
		}
		return null;
	}

	public static final Pattern DATE_FORMAT = Pattern.compile("(\\d{4})\\D(\\d{1,2})\\D(\\d{1,2})");

}
