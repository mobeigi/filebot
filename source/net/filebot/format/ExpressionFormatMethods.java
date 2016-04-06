package net.filebot.format;

import static java.util.regex.Pattern.*;
import static net.filebot.format.ExpressionFormatFunctions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;

import com.ibm.icu.text.Transliterator;

import groovy.lang.Closure;
import net.filebot.Language;
import net.filebot.similarity.Normalization;
import net.filebot.util.FileUtilities;
import net.filebot.util.StringUtilities;
import net.filebot.web.SimpleDate;

public class ExpressionFormatMethods {

	/**
	 * Convenience methods for String.toLowerCase() and String.toUpperCase()
	 */
	public static String lower(String self) {
		return self.toLowerCase();
	}

	public static String upper(String self) {
		return self.toUpperCase();
	}

	/**
	 * Pad strings or numbers with given characters ('0' by default).
	 *
	 * e.g. "1" -> "01"
	 */
	public static String pad(String self, int length, String padding) {
		while (self.length() < length) {
			self = padding + self;
		}
		return self;
	}

	public static String pad(String self, int length) {
		return pad(self, length, "0");
	}

	public static String pad(Number self, int length) {
		return pad(self.toString(), length, "0");
	}

	/**
	 * Return a substring matching the given pattern or break.
	 */
	public static String match(String self, String pattern) throws Exception {
		return match(self, pattern, -1);
	}

	public static String match(String self, String pattern, int matchGroup) throws Exception {
		Matcher matcher = compile(pattern, CASE_INSENSITIVE | UNICODE_CHARACTER_CLASS | MULTILINE).matcher(self);
		if (matcher.find()) {
			return firstCapturingGroup(matcher, matchGroup);
		} else {
			throw new Exception("Pattern not found");
		}
	}

	/**
	 * Return a list of all matching patterns or break.
	 */
	public static List<String> matchAll(String self, String pattern) throws Exception {
		return matchAll(self, pattern, -1);
	}

	public static List<String> matchAll(String self, String pattern, int matchGroup) throws Exception {
		List<String> matches = new ArrayList<String>();
		Matcher matcher = compile(pattern, CASE_INSENSITIVE | UNICODE_CHARACTER_CLASS | MULTILINE).matcher(self);
		while (matcher.find()) {
			matches.add(firstCapturingGroup(matcher, matchGroup));
		}

		if (matches.isEmpty()) {
			throw new Exception("Pattern not found");
		}
		return matches;
	}

	public static String firstCapturingGroup(Matcher self, int matchGroup) throws Exception {
		int g = matchGroup < 0 ? self.groupCount() > 0 ? 1 : 0 : matchGroup;

		// return the entire match
		if (g == 0) {
			return self.group();
		}

		// otherwise find first non-empty capturing group
		return IntStream.rangeClosed(g, self.groupCount()).mapToObj(self::group).filter(Objects::nonNull).map(String::trim).filter(s -> s.length() > 0).findFirst().orElseThrow(() -> {
			return new Exception(String.format("Capturing group %d not found", g));
		});
	}

	public static String replaceAll(String self, String pattern) {
		return self.replaceAll(pattern, "");
	}

	public static String removeAll(String self, String pattern) {
		return compile(pattern, CASE_INSENSITIVE | UNICODE_CHARACTER_CLASS | MULTILINE).matcher(self).replaceAll("").trim();
	}

	public static String removeIllegalCharacters(String self) {
		return FileUtilities.validateFileName(Normalization.normalizeQuotationMarks(self));
	}

	/**
	 * Replace space characters with a given characters.
	 *
	 * e.g. "Doctor Who" -> "Doctor_Who"
	 */
	public static String space(String self, String replacement) {
		self = self.replaceAll("[:?._]", " ").trim();

		// replace space sequences with a single blank
		return Normalization.replaceSpace(self, replacement);
	}

	/**
	 * Replace colon to make the name more Windows friendly.
	 *
	 * e.g. "Sissi: The Young Empress" -> "Sissi - The Young Empress"
	 */
	public static String colon(String self, String replacement) {
		return compile("\\s*[:]\\s*", UNICODE_CHARACTER_CLASS).matcher(self).replaceAll(replacement);
	}

	/**
	 * Upper-case all initials.
	 *
	 * e.g. "The Day a new Demon was born" -> "The Day A New Demon Was Born"
	 */
	public static String upperInitial(String self) {
		Matcher matcher = compile("(?<=[&()+.,-;<=>?\\[\\]_{|}~ ]|^)[a-z]").matcher(self);

		StringBuffer buffer = new StringBuffer();
		while (matcher.find()) {
			matcher.appendReplacement(buffer, matcher.group().toUpperCase());
		}
		matcher.appendTail(buffer);

		return buffer.toString();
	}

	public static String sortName(String self) {
		return sortName(self, "$2");
	}

	public static String sortName(String self, String replacement) {
		return compile("^(The|A|An)\\s(.+)", CASE_INSENSITIVE | UNICODE_CHARACTER_CLASS).matcher(self).replaceFirst(replacement).trim();
	}

	public static String sortInitial(String self) {
		// use primary initial, ignore The XY, A XY, etc
		String s = ascii(sortName(self)).toUpperCase();
		int c = s.codePointAt(0);

		if (Character.isDigit(c)) {
			return "0-9";
		}
		if (Character.isLetter(c)) {
			return String.valueOf(Character.toChars(c));
		}

		return null;
	}

	/**
	 * Get acronym, i.e. first letter of each word.
	 *
	 * e.g. "Deep Space 9" -> "DS9"
	 */
	public static String acronym(String self) {
		String name = sortName(self, "$2");
		Matcher matcher = compile("(?<=[&()+.,-;<=>?\\[\\]_{|}~ ]|^)[\\p{Alnum}]").matcher(name);

		StringBuilder buffer = new StringBuilder();
		while (matcher.find()) {
			buffer.append(matcher.group().toUpperCase());
		}

		return buffer.toString();
	}

	/**
	 * Lower-case all letters that are not initials.
	 *
	 * e.g. "Gundam SEED" -> "Gundam Seed"
	 */
	public static String lowerTrail(String self) {
		Matcher matcher = compile("\\b(\\p{Alpha})(\\p{Alpha}+)\\b").matcher(self);

		StringBuffer buffer = new StringBuffer();
		while (matcher.find()) {
			matcher.appendReplacement(buffer, matcher.group(1) + matcher.group(2).toLowerCase());
		}
		matcher.appendTail(buffer);

		return buffer.toString();
	}

	public static String truncate(String self, int limit) {
		if (limit >= self.length())
			return self;

		return self.substring(0, limit);
	}

	public static String truncate(String self, int hardLimit, String nonWordPattern) {
		if (hardLimit >= self.length())
			return self;

		int softLimit = 0;
		Matcher matcher = compile(nonWordPattern, CASE_INSENSITIVE | UNICODE_CHARACTER_CLASS).matcher(self);
		while (matcher.find()) {
			if (matcher.start() > hardLimit)
				break;

			softLimit = matcher.start();
		}
		return truncate(self, softLimit);
	}

	/**
	 * Return substring before the given pattern.
	 */
	public static String before(String self, String pattern) {
		Matcher matcher = compile(pattern, CASE_INSENSITIVE | UNICODE_CHARACTER_CLASS).matcher(self);

		// pattern was found, return leading substring, else return original value
		return matcher.find() ? self.substring(0, matcher.start()).trim() : self;
	}

	/**
	 * Return substring after the given pattern.
	 */
	public static String after(String self, String pattern) {
		Matcher matcher = compile(pattern, CASE_INSENSITIVE | UNICODE_CHARACTER_CLASS).matcher(self);

		// pattern was found, return trailing substring, else return original value
		return matcher.find() ? self.substring(matcher.end(), self.length()).trim() : self;
	}

	/**
	 * Find a matcher that matches the given pattern (case-insensitive)
	 */
	public static Matcher findMatch(String self, String pattern) {
		if (pattern != null) {
			Matcher matcher = compile(pattern, CASE_INSENSITIVE | UNICODE_CHARACTER_CLASS).matcher(self);
			if (matcher.find()) {
				return matcher.reset();
			}
		}
		return null;
	}

	/**
	 * Replace trailing parenthesis including any leading whitespace.
	 *
	 * e.g. "The IT Crowd (UK)" -> "The IT Crowd"
	 */
	public static String replaceTrailingBrackets(String self) {
		return replaceTrailingBrackets(self, "");
	}

	public static String replaceTrailingBrackets(String self, String replacement) {
		return compile("\\s*[(]([^)]*)[)]$", UNICODE_CHARACTER_CLASS).matcher(self).replaceAll(replacement).trim();
	}

	/**
	 * Replace 'part identifier'.
	 *
	 * e.g. "Today Is the Day: Part 1" -> "Today Is the Day, Part 1" or "Today Is the Day (1)" -> "Today Is the Day, Part 1"
	 */
	public static String replacePart(String self) {
		return replacePart(self, "");
	}

	public static String replacePart(String self, String replacement) {
		// handle '(n)', '(Part n)' and ': Part n' like syntax
		String[] patterns = new String[] { "\\s*[(](\\w+)[)]$", "\\W+Part (\\w+)\\W*$" };

		for (String pattern : patterns) {
			Matcher matcher = compile(pattern, CASE_INSENSITIVE | UNICODE_CHARACTER_CLASS).matcher(self);
			if (matcher.find()) {
				return matcher.replaceAll(replacement).trim();
			}
		}

		// no pattern matches, nothing to replace
		return self;
	}

	/**
	 * Apply ICU transliteration
	 *
	 * @see http://userguide.icu-project.org/transforms/general
	 */
	public static String transliterate(String self, String transformIdentifier) {
		return Transliterator.getInstance(transformIdentifier).transform(self);
	}

	/**
	 * Convert Unicode to ASCII as best as possible. Works with most alphabets/scripts used in the world.
	 *
	 * e.g. "Österreich" -> "Osterreich" "カタカナ" -> "katakana"
	 */
	public static String ascii(String self) {
		return ascii(self, " ");
	}

	public static String ascii(String self, String fallback) {
		return Transliterator.getInstance("Any-Latin;Latin-ASCII;[:Diacritic:]remove").transform(asciiQuotes(self)).replaceAll("[^\\p{ASCII}]+", fallback).trim();
	}

	public static String asciiQuotes(String self) {
		return Normalization.normalizeQuotationMarks(self);
	}

	/**
	 * Replace multiple replacement pairs
	 *
	 * e.g. replace(ä:'ae', ö:'oe', ü:'ue')
	 */
	public static String replace(String self, Map<?, ?> replace) {
		// the first two parameters are required, the rest of the parameter sequence is optional
		for (Entry<?, ?> it : replace.entrySet()) {
			if (it.getKey() instanceof Pattern) {
				self = ((Pattern) it.getKey()).matcher(self).replaceAll(it.getValue().toString());
			} else {
				self = self.replace(it.getKey().toString(), it.getValue().toString());
			}
		}
		return self;
	}

	/**
	 * Join non-empty String values and prepend prefix / append suffix values
	 *
	 * e.g. (1..3).join('-', '[', ']')
	 *
	 * Unwind if list is empty
	 *
	 * e.g. [].join('-', '[', ']') => Exception: List is empty
	 */
	public static String join(Collection<?> self, String delimiter, String prefix, String suffix) throws Exception {
		String[] values = self.stream().map(StringUtilities::asNonEmptyString).filter(Objects::nonNull).toArray(String[]::new);
		if (values.length > 0) {
			return prefix + String.join(delimiter, values) + suffix;
		}
		throw new Exception("List is empty");
	}

	/**
	 * Unwind if an object does not satisfy the given predicate
	 *
	 * e.g. (0..9)*.check{it < 10}.sum()
	 */
	public static Object check(Object self, Closure<?> c) throws Exception {
		if (DefaultTypeTransformation.castToBoolean(c.call(self))) {
			return self;
		}
		throw new Exception("Check failed");
	}

	/**
	 * File utilities
	 */
	public static File getRoot(File self) {
		return FileUtilities.listPath(self).get(0);
	}

	public static List<File> listPath(File self) {
		return FileUtilities.listPath(self);
	}

	public static List<File> listPath(File self, int tailSize) {
		return FileUtilities.listPath(FileUtilities.getRelativePathTail(self, tailSize));
	}

	public static File getRelativePathTail(File self, int tailSize) {
		return FileUtilities.getRelativePathTail(self, tailSize);
	}

	public static long getDiskSpace(File self) {
		List<File> list = FileUtilities.listPath(self);
		for (int i = list.size() - 1; i >= 0; i--) {
			if (list.get(i).exists()) {
				long usableSpace = list.get(i).getUsableSpace();
				if (usableSpace > 0) {
					return usableSpace;
				}
			}
		}
		return 0;
	}

	public static long getCreationDate(File self) {
		try {
			BasicFileAttributes attr = Files.getFileAttributeView(self.toPath(), BasicFileAttributeView.class).readAttributes();
			long creationDate = attr.creationTime().toMillis();
			if (creationDate > 0) {
				return creationDate;
			}
			return attr.lastModifiedTime().toMillis();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static File toFile(String self) {
		if (self == null || self.isEmpty()) {
			return null;
		}
		return new File(self);
	}

	public static File toFile(String self, String parent) {
		if (self == null || self.isEmpty()) {
			return null;
		}
		File file = new File(self);
		if (file.isAbsolute()) {
			return file;
		}
		return new File(parent, self);
	}

	public static Locale toLocale(String self) {
		return Locale.forLanguageTag(self);
	}

	public static String plus(String self, Closure<?> other) {
		return concat(self, other);
	}

	public static String plus(Closure<?> self, Object other) {
		return concat(self, other);
	}

	public static String plus(Language self, Object other) {
		return concat(self, other);
	}

	public static String plus(SimpleDate self, Object other) {
		return concat(self, other);
	}

}
