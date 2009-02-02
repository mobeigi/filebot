
package net.sourceforge.filebot;


import java.io.File;
import java.io.FileFilter;
import java.util.AbstractList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.tuned.FileUtilities;
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
	

	public static String join(Object[] values, String separator) {
		StringBuilder sb = new StringBuilder();
		
		for (int i = 0; i < values.length; i++) {
			sb.append(values[i]);
			
			if (i < values.length - 1) {
				sb.append(separator);
			}
		}
		
		return sb.toString();
	}
	

	public static List<String> asFileNameList(final List<File> list) {
		return new AbstractList<String>() {
			
			@Override
			public String get(int index) {
				return FileUtilities.getName(list.get(index));
			}
			

			@Override
			public int size() {
				return list.size();
			}
		};
	}
	
	public static final FileFilter TORRENT_FILES = new ExtensionFileFilter("torrent");
	public static final FileFilter SFV_FILES = new ExtensionFileFilter("sfv");
	public static final FileFilter LIST_FILES = new ExtensionFileFilter("txt", "list", "");
	public static final FileFilter SUBTITLE_FILES = new ExtensionFileFilter("srt", "sub", "ssa", "ass", "smi");
	
	
	/**
	 * Dummy constructor to prevent instantiation.
	 */
	private FileBotUtilities() {
		throw new UnsupportedOperationException();
	}
	
}
