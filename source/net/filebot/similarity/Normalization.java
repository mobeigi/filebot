package net.filebot.similarity;

import static java.util.regex.Pattern.*;

import java.util.regex.Pattern;

public class Normalization {

	private static final Pattern apostrophe = compile("['`´‘’ʻ]+");
	private static final Pattern punctuation = compile("[\\p{Punct}\\p{Space}]+");

	private static final Pattern space = compile("\\s+");
	private static final Pattern spaceLikePunctuation = compile("[:?._]");

	private static final Pattern[] brackets = new Pattern[] { compile("\\([^\\(]*\\)"), compile("\\[[^\\[]*\\]"), compile("\\{[^\\{]*\\}") };
	private static final Pattern trailingParentheses = compile("(?<!^)[(]([^)]*)[)]$");

	private static final Pattern checksum = compile("[\\(\\[]\\p{XDigit}{8}[\\]\\)]");

	private static final char[] doubleQuotes = new char[] { '\"', '\u0060', '\u00b4', '\u2018', '\u2019', '\u02bb' };
	private static final char[] singleQuotes = new char[] { '\'', '\u201c', '\u201d' };

	public static String normalizeQuotationMarks(String name) {
		for (char[] cs : new char[][] { doubleQuotes, singleQuotes }) {
			for (char c : cs) {
				name = name.replace(c, cs[0]);
			}
		}
		return name;
	}

	public static String normalizePunctuation(String name) {
		// remove/normalize special characters
		name = apostrophe.matcher(name).replaceAll("");
		name = punctuation.matcher(name).replaceAll(" ");
		return name.trim();
	}

	public static String normalizeBrackets(String name) {
		// remove group names and checksums, any [...] or (...)
		for (Pattern it : brackets) {
			name = it.matcher(name).replaceAll(" ");
		}

		return name;
	}

	public static String normalizeSpace(String name, String replacement) {
		return replaceSpace(spaceLikePunctuation.matcher(name).replaceAll(" ").trim(), replacement);
	}

	public static String replaceSpace(String name, String replacement) {
		return space.matcher(name).replaceAll(replacement);
	}

	public static String removeEmbeddedChecksum(String name) {
		// match embedded checksum and surrounding brackets
		return checksum.matcher(name).replaceAll("");
	}

	public static String removeTrailingBrackets(String name) {
		// remove trailing braces, e.g. Doctor Who (2005) -> Doctor Who
		return trailingParentheses.matcher(name).replaceAll("").trim();
	}

}
