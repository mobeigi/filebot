package net.sourceforge.filebot.similarity;

import static java.util.regex.Pattern.*;

import java.util.regex.Pattern;

public class Normalization {

	private static final Pattern apostrophe = compile("['`´‘’ʻ]+");
	private static final Pattern punctuation = compile("[\\p{Punct}\\p{Space}]+");

	private static final Pattern[] brackets = new Pattern[] { compile("\\([^\\(]*\\)"), compile("\\[[^\\[]*\\]"), compile("\\{[^\\{]*\\}") };
	private static final Pattern trailingParentheses = compile("(?<!^)[(]([^)]*)[)]$");

	private static final Pattern checksum = compile("[\\(\\[]\\p{XDigit}{8}[\\]\\)]");

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

	public static String removeEmbeddedChecksum(String string) {
		// match embedded checksum and surrounding brackets
		return checksum.matcher(string).replaceAll("");
	}

	public static String removeTrailingBrackets(String name) {
		// remove trailing braces, e.g. Doctor Who (2005) -> Doctor Who
		return trailingParentheses.matcher(name).replaceAll("").trim();
	}

}
