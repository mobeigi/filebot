
package net.sourceforge.filebot.hash;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;


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
	

	public static VerificationFileReader createVerificationFileReader(File file, HashType type) throws IOException {
		// detect charset and read text content
		CharsetDetector detector = new CharsetDetector();
		detector.setDeclaredEncoding("UTF-8");
		detector.setText(new BufferedInputStream(new FileInputStream(file)));
		
		CharsetMatch charset = detector.detect();
		Reader source = (charset != null) ? charset.getReader() : new InputStreamReader(new FileInputStream(file), "UTF-8");
		
		return new VerificationFileReader(source, type.getFormat());
	}
	

	private static String getHashFromVerificationFile(File folder, File target, HashType type, int depth, int maxDepth) throws IOException {
		// stop if we reached max depth or the file system root
		if (folder == null || depth > maxDepth)
			return null;
		
		// scan all sfv files in this folder
		for (File verificationFile : folder.listFiles(type.getFilter())) {
			VerificationFileReader parser = createVerificationFileReader(verificationFile, type);
			
			try {
				while (parser.hasNext()) {
					Entry<File, String> entry = parser.next();
					
					// resolve relative file path
					File file = new File(folder, entry.getKey().getPath());
					
					if (target.equals(file)) {
						return entry.getValue();
					}
				}
			} finally {
				parser.close();
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
