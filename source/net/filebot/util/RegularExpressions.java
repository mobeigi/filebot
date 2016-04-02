package net.filebot.util;

import static java.util.regex.Pattern.*;

import java.util.regex.Pattern;

public class RegularExpressions {

	public static final Pattern DIGIT = compile("\\d+");
	public static final Pattern NON_DIGIT = compile("\\D+");

	public static final Pattern PIPE = compile("|", LITERAL);
	public static final Pattern EQUALS = compile("=", LITERAL);
	public static final Pattern TAB = compile("\t", LITERAL);
	public static final Pattern SEMICOLON = compile(";", LITERAL);

	public static final Pattern COMMA = compile("\\s*[,;:]\\s*", UNICODE_CHARACTER_CLASS);
	public static final Pattern SLASH = compile("\\s*[\\\\/]+\\s*", UNICODE_CHARACTER_CLASS);
	public static final Pattern SPACE = compile("\\s+", UNICODE_CHARACTER_CLASS); // French No-Break Space U+00A0

}
