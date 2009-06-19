
package net.sourceforge.filebot;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.tuned.FileUtilities.ExtensionFileFilter;


public final class FileBotUtilities {
	
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
	public static String validateFileName(CharSequence filename) {
		// strip invalid characters from filename
		return INVALID_CHARACTERS_PATTERN.matcher(filename).replaceAll("");
	}
	

	public static boolean isInvalidFileName(CharSequence filename) {
		return INVALID_CHARACTERS_PATTERN.matcher(filename).find();
	}
	

	/**
	 * A {@link Pattern} that will match checksums enclosed in brackets ("[]" or "()"). A
	 * checksum string is a hex number with at least 8 digits. Capturing group 0 will contain
	 * the matched checksum string.
	 */
	public static final Pattern EMBEDDED_CHECKSUM_PATTERN = Pattern.compile("(?<=\\[|\\()(\\p{XDigit}{8,})(?=\\]|\\))");
	

	public static String getEmbeddedChecksum(CharSequence string) {
		Matcher matcher = EMBEDDED_CHECKSUM_PATTERN.matcher(string);
		String embeddedChecksum = null;
		
		// get last match
		while (matcher.find()) {
			embeddedChecksum = matcher.group();
		}
		
		return embeddedChecksum;
	}
	

	public static String removeEmbeddedChecksum(String string) {
		return string.replaceAll("[\\(\\[]\\p{XDigit}{8}[\\]\\)]", "");
	}
	

	public static final ExtensionFileFilter TORRENT_FILES = MediaTypes.getDefault().filter("application/torrent");
	public static final ExtensionFileFilter LIST_FILES = MediaTypes.getDefault().filter("application/list");
	public static final ExtensionFileFilter VIDEO_FILES = MediaTypes.getDefault().filter("video");
	public static final ExtensionFileFilter SUBTITLE_FILES = MediaTypes.getDefault().filter("subtitle");
	public static final ExtensionFileFilter SFV_FILES = MediaTypes.getDefault().filter("verification/sfv");
	

	/**
	 * Dummy constructor to prevent instantiation.
	 */
	private FileBotUtilities() {
		throw new UnsupportedOperationException();
	}
	
}
