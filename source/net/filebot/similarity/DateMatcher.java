package net.filebot.similarity;

import static java.util.Arrays.*;
import static java.util.stream.Collectors.*;
import static net.filebot.util.FileUtilities.*;

import java.io.File;
import java.time.LocalDate;
import java.time.Month;
import java.time.chrono.ChronoLocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import net.filebot.web.SimpleDate;

public class DateMatcher {

	public static final DateFilter DEFAULT_SANITY = new DateFilter(LocalDate.of(1930, Month.JANUARY, 1), LocalDate.of(2050, Month.JANUARY, 1));

	private final DatePattern[] patterns;

	public DateMatcher(Locale locale, DateFilter sanity) {
		// generate default date format patterns
		List<String> format = new ArrayList<String>(7);

		// match yyyy-mm-dd patterns like 2010-10-24, 2009/6/1, etc
		format.add("y M d");

		// match dd-mm-yyyy patterns like 1.1.2010, 01/06/2010, etc
		format.add("d M y");

		// match yyyy.MMMMM.dd patterns like 2015.October.05
		format.add("y MMMM d");

		// match yyyy.MMM.dd patterns like 2015.Oct.6
		format.add("y MMM d");

		// match dd.MMMMM.yyyy patterns like 25 July 2014
		format.add("d MMMM y");

		// match dd.MMM.yyyy patterns like 8 Sep 2015
		format.add("d MMM y");

		// match yyyymmdd patterns like 20140408
		format.add("yyyyMMdd");

		this.patterns = compile(format, locale, sanity);
	}

	protected DatePattern[] compile(List<String> dateFormat, Locale locale, DateFilter sanity) {
		return dateFormat.stream().map(format -> {
			String pattern = stream(format.split(DateFormatPattern.DELIMITER)).map(this::getPatternGroup).collect(joining("[^\\p{Alnum}]", "(?<!\\p{Alnum})", "(?!\\p{Alnum})"));
			return new DateFormatPattern(pattern, format, locale, sanity);
		}).toArray(DateFormatPattern[]::new);
	}

	protected String getPatternGroup(String token) {
		switch (token) {
		case "y":
			return "(\\d{4})";
		case "M":
			return "(\\d{1,2})";
		case "d":
			return "(\\d{1,2})";
		case "yyyyMMdd":
			return "(\\d{8})";
		case "MMMM":
			return "(January|February|March|April|May|June|July|August|September|October|November|December)";
		case "MMM":
			return "(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)";
		default:
			throw new IllegalArgumentException(token);
		}
	}

	public SimpleDate match(CharSequence seq) {
		for (DatePattern pattern : patterns) {
			SimpleDate match = pattern.match(seq);

			if (match != null) {
				return match;
			}
		}
		return null;
	}

	public int find(CharSequence seq, int fromIndex) {
		for (DatePattern pattern : patterns) {
			int pos = pattern.find(seq, fromIndex);

			if (pos >= 0) {
				return pos;
			}
		}
		return -1;
	}

	public SimpleDate match(File file) {
		for (String name : tokenizeTail(file)) {
			for (DatePattern pattern : patterns) {
				SimpleDate match = pattern.match(name);

				if (match != null) {
					return match;
				}
			}
		}
		return null;
	}

	protected List<String> tokenizeTail(File file) {
		List<String> tail = new ArrayList<String>(2);
		for (File f : listPathTail(file, 2, true)) {
			tail.add(getName(f));
		}
		return tail;
	}

	public static interface DatePattern {

		public SimpleDate match(CharSequence seq);

		public int find(CharSequence seq, int fromIndex);

	}

	public static class DateFormatPattern implements DatePattern {

		public static final String DELIMITER = " ";

		protected final Pattern pattern;
		protected final DateTimeFormatter format;
		protected final DateFilter sanity;

		public DateFormatPattern(String pattern, String format, Locale locale, DateFilter sanity) {
			this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
			this.format = DateTimeFormatter.ofPattern(format, locale);
			this.sanity = sanity;
		}

		protected SimpleDate process(MatchResult match) {
			try {
				String dateString = IntStream.rangeClosed(1, match.groupCount()).mapToObj(match::group).collect(joining(DELIMITER));
				LocalDate date = LocalDate.parse(dateString, format);

				if (sanity == null || sanity.test(date)) {
					return new SimpleDate(date.getYear(), date.getMonthValue(), date.getDayOfMonth());
				}
			} catch (DateTimeParseException e) {
				// date is invalid
			}
			return null;
		}

		@Override
		public SimpleDate match(CharSequence seq) {
			Matcher matcher = pattern.matcher(seq);

			if (matcher.find()) {
				return process(matcher);
			}
			return null;
		}

		@Override
		public int find(CharSequence seq, int fromIndex) {
			Matcher matcher = pattern.matcher(seq).region(fromIndex, seq.length());

			if (matcher.find()) {
				if (process(matcher) != null) {
					return matcher.start();
				}
			}
			return -1;
		}

	}

	public static class DateFilter implements Predicate<ChronoLocalDate> {

		public final ChronoLocalDate lowerBound;
		public final ChronoLocalDate upperBound;

		public DateFilter(ChronoLocalDate lowerBound, ChronoLocalDate upperBound) {
			this.lowerBound = lowerBound;
			this.upperBound = upperBound;
		}

		@Override
		public boolean test(ChronoLocalDate date) {
			return date.isAfter(lowerBound) && date.isBefore(upperBound);
		}

		public boolean acceptDate(int year, int month, int day) {
			return test(LocalDate.of(year, month, day));
		}

		public boolean acceptYear(int year) {
			return test(LocalDate.of(year, 1, 1));
		}

	}

}
