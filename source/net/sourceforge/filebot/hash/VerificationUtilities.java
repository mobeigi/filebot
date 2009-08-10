
package net.sourceforge.filebot.hash;


import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class VerificationUtilities {
	
	/**
	 * A {@link Pattern} that will match checksums enclosed in brackets ("[]" or "()"). A
	 * checksum string is a hex number with at least 8 digits. Capturing group 0 will contain
	 * the matched checksum string.
	 */
	public static final Pattern EMBEDDED_CHECKSUM = Pattern.compile("(?<=\\[|\\()(\\p{XDigit}{8})(?=\\]|\\))");
	

	public static String getEmbeddedChecksum(CharSequence string) {
		Matcher matcher = EMBEDDED_CHECKSUM.matcher(string);
		String embeddedChecksum = null;
		
		// get last match
		while (matcher.find()) {
			embeddedChecksum = matcher.group();
		}
		
		return embeddedChecksum;
	}
	

	public static String removeEmbeddedChecksum(String string) {
		// match embedded checksum and surrounding brackets 
		return string.replaceAll("[\\(\\[]\\p{XDigit}{8}[\\]\\)]", "");
	}
	

	public static String getHashFromVerificationFile(File file, HashType type, int maxDepth) throws IOException {
		return getHashFromVerificationFile(file.getParentFile(), file, type, 0, maxDepth);
	}
	

	private static String getHashFromVerificationFile(File folder, File target, HashType type, int depth, int maxDepth) throws IOException {
		// stop if we reached max depth or the file system root
		if (folder == null || depth > maxDepth)
			return null;
		
		// scan all sfv files in this folder
		for (File verificationFile : folder.listFiles(type.getFilter())) {
			VerificationFileReader scanner = new VerificationFileReader(verificationFile, type.getFormat());
			
			try {
				while (scanner.hasNext()) {
					Entry<File, String> entry = scanner.next();
					
					// resolve relative file path
					File file = new File(folder, entry.getKey().getPath());
					
					if (target.equals(file)) {
						return entry.getValue();
					}
				}
			} finally {
				scanner.close();
			}
		}
		
		return getHashFromVerificationFile(folder.getParentFile(), target, type, depth + 1, maxDepth);
	}
	

	public static HashType getHashType(File verificationFile) {
		for (HashType hashType : HashType.values()) {
			if (hashType.getFilter().accept(verificationFile))
				return hashType;
		}
		
		return null;
	}
	

	/**
	 * Dummy constructor to prevent instantiation.
	 */
	private VerificationUtilities() {
		throw new UnsupportedOperationException();
	}
	
}
