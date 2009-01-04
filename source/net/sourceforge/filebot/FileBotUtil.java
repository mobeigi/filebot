
package net.sourceforge.filebot;


import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.tuned.FileUtil;


public final class FileBotUtil {
	
	public static String getApplicationName() {
		return "FileBot";
	};
	

	public static String getApplicationVersion() {
		return "1.9";
	};
	
	/**
	 * Invalid characters in filenames: \, /, :, *, ?, ", <, >, |, \r and \n
	 */
	public static final String INVALID_CHARACTERS = "\\/:*?\"<>|\r\n";
	public static final Pattern INVALID_CHARACTERS_PATTERN = Pattern.compile(String.format("[%s]+", Pattern.quote(INVALID_CHARACTERS)));
	
	
	/**
	 * Strip filename of invalid characters
	 * 
	 * @param filename original filename
	 * @return valid filename stripped of invalid characters
	 */
	public static String validateFileName(String filename) {
		// strip invalid characters from filename
		return INVALID_CHARACTERS_PATTERN.matcher(filename).replaceAll("");
	}
	

	public static boolean isInvalidFileName(String filename) {
		return INVALID_CHARACTERS_PATTERN.matcher(filename).find();
	}
	
	/**
	 * A {@link Pattern} that will match checksums enclosed in brackets ("[]" or "()"). A
	 * checksum string is a hex number with at least 8 digits. Capturing group 0 will contain
	 * the matched checksum string.
	 */
	public static final Pattern EMBEDDED_CHECKSUM_PATTERN = Pattern.compile("(?<=\\[|\\()(\\p{XDigit}{8,})(?=\\]|\\))");
	
	
	public static String getEmbeddedChecksum(String string) {
		Matcher matcher = EMBEDDED_CHECKSUM_PATTERN.matcher(string);
		String embeddedChecksum = null;
		
		// get last match
		while (matcher.find()) {
			embeddedChecksum = matcher.group(0);
		}
		
		return embeddedChecksum;
	}
	
	public static final List<String> TORRENT_FILE_EXTENSIONS = unmodifiableList("torrent");
	public static final List<String> SFV_FILE_EXTENSIONS = unmodifiableList("sfv");
	public static final List<String> LIST_FILE_EXTENSIONS = unmodifiableList("txt", "list", "");
	public static final List<String> SUBTITLE_FILE_EXTENSIONS = unmodifiableList("srt", "sub", "ssa", "smi");
	
	
	private static List<String> unmodifiableList(String... elements) {
		return Collections.unmodifiableList(Arrays.asList(elements));
	}
	

	public static boolean containsOnlyFolders(Iterable<File> files) {
		for (File file : files) {
			if (!file.isDirectory())
				return false;
		}
		
		return true;
	}
	

	public static boolean containsOnly(Iterable<File> files, Iterable<String> extensions) {
		for (File file : files) {
			if (!FileUtil.hasExtension(file, extensions))
				return false;
		}
		
		return true;
	}
	

	/**
	 * Dummy constructor to prevent instantiation.
	 */
	private FileBotUtil() {
		throw new UnsupportedOperationException();
	}
	
}
