package net.filebot.format;

import static java.util.regex.Pattern.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import net.filebot.util.FileUtilities;

import com.ibm.icu.text.Transliterator;

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
	public static String match(String self, String pattern) {
		return match(self, pattern, -1);
	}

	public static String match(String self, String pattern, int matchGroup) {
		Matcher matcher = compile(pattern, CASE_INSENSITIVE | UNICODE_CHARACTER_CLASS | MULTILINE).matcher(self);
		if (matcher.find()) {
			return (matcher.groupCount() > 0 && matchGroup < 0 ? matcher.group(1) : matcher.group(matchGroup < 0 ? 0 : matchGroup)).trim();
		} else {
			throw new IllegalArgumentException("Pattern not found");
		}
	}

	/**
	 * Return a list of all matching patterns or break.
	 */
	public static List<String> matchAll(String self, String pattern) {
		return matchAll(self, pattern, 0);
	}

	public static List<String> matchAll(String self, String pattern, int matchGroup) {
		List<String> matches = new ArrayList<String>();
		Matcher matcher = compile(pattern, CASE_INSENSITIVE | UNICODE_CHARACTER_CLASS | MULTILINE).matcher(self);
		while (matcher.find()) {
			matches.add(matcher.group(matchGroup).trim());
		}

		if (matches.size() > 0) {
			return matches;
		} else {
			throw new IllegalArgumentException("Pattern not found");
		}
	}

	public static String replaceAll(String self, String pattern) {
		return self.replaceAll(pattern, "");
	}

	public static String removeAll(String self, String pattern) {
		return compile(pattern, CASE_INSENSITIVE | UNICODE_CHARACTER_CLASS | MULTILINE).matcher(self).replaceAll("").trim();
	}

	/**
	 * Replace space characters with a given characters.
	 * 
	 * e.g. "Doctor Who" -> "Doctor_Who"
	 */
	public static String space(String self, String replacement) {
		return self.replaceAll("[:?._]", " ").trim().replaceAll("\\s+", replacement);
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
		return sortName(self, "$2, $1");
	}

	public static String sortName(String self, String replacement) {
		return compile("^(The|A|An)\\s(.+)", CASE_INSENSITIVE).matcher(self).replaceFirst(replacement).trim();
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
	 * Find MatchResult that matches the given pattern (case-insensitive)
	 */
	public static Matcher findMatch(String self, String pattern) {
		Matcher matcher = compile(pattern, CASE_INSENSITIVE | UNICODE_CHARACTER_CLASS).matcher(self);
		return matcher.find() ? matcher.reset() : null;
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
		return self.replaceAll("\\s*[(]([^)]*)[)]$", replacement).trim();
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
			Matcher matcher = compile(pattern, CASE_INSENSITIVE).matcher(self);
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
		return Transliterator.getInstance("Any-Latin;Latin-ASCII;[:Diacritic:]remove").transform(self).replaceAll("[^\\p{ASCII}]+", fallback).trim();
	}

	/**
	 * Replace multiple replacement pairs
	 * 
	 * e.g. replace('ä', 'ae', 'ö', 'oe', 'ü', 'ue')
	 */
	public static String replace(String self, String tr0, String tr1, String... tr) {
		// the first two parameters are required, the rest of the parameter sequence is optional
		self = self.replace(tr0, tr1);

		for (int i = 0; i < tr.length - 1; i += 2) {
			String t = tr[i];
			String r = tr[i + 1];
			self = self.replace(t, r);
		}

		return self;
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

	public static long getCreationDate(File self) throws IOException {
		BasicFileAttributes attr = Files.getFileAttributeView(self.toPath(), BasicFileAttributeView.class).readAttributes();
		long creationDate = attr.creationTime().toMillis();
		if (creationDate > 0) {
			return creationDate;
		}
		return attr.lastModifiedTime().toMillis();
	}

	public static File toFile(String self) {
		return new File(self);
	}

}
