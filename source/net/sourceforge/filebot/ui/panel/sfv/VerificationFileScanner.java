
package net.sourceforge.filebot.ui.panel.sfv;


import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


class VerificationFileScanner implements Iterator<Entry<File, String>>, Closeable {
	
	private final Scanner scanner;
	
	private String cache;
	
	private int lineNumber = 0;
	
	
	public VerificationFileScanner(File file) throws FileNotFoundException {
		// don't use new Scanner(File) because of BUG 6368019 (http://bugs.sun.com/view_bug.do?bug_id=6368019)
		this(new Scanner(new FileInputStream(file), "UTF-8"));
	}
	

	public VerificationFileScanner(Scanner scanner) {
		this.scanner = scanner;
	}
	

	@Override
	public boolean hasNext() {
		if (cache == null) {
			// cache next line
			cache = nextLine();
		}
		
		return cache != null;
	}
	

	@Override
	public Entry<File, String> next() {
		// cache next line
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		
		try {
			return parseLine(cache);
		} finally {
			// invalidate cache
			cache = null;
		}
	}
	

	protected String nextLine() {
		String line = null;
		
		// get next non-comment line
		while (scanner.hasNext() && (line == null || isComment(line))) {
			line = scanner.nextLine().trim();
			lineNumber++;
		}
		
		return line;
	}
	
	/**
	 * Pattern used to parse the lines of a md5 or sha1 file.
	 * 
	 * <pre>
	 * Sample MD5:
	 * 50e85fe18e17e3616774637a82968f4c *folder/file.txt
	 * |           Group 1               |   Group 2   |
	 * 
	 * Sample SHA-1:
	 * 1a02a7c1e9ac91346d08829d5037b240f42ded07 ?SHA1*folder/file.txt
	 * |               Group 1                |       |   Group 2   |
	 * </pre>
	 */
	private final Pattern pattern = Pattern.compile("(\\p{XDigit}{8,})\\s+(?:\\?\\w+)?\\*(.+)");
	
	
	protected Entry<File, String> parseLine(String line) {
		Matcher matcher = pattern.matcher(line);
		
		if (!matcher.matches())
			throw new IllegalSyntaxException(getLineNumber(), line);
		
		return entry(new File(matcher.group(2)), matcher.group(1));
	}
	

	public int getLineNumber() {
		return lineNumber;
	}
	

	protected boolean isComment(String line) {
		return line.isEmpty() || line.startsWith(";");
	}
	

	protected Entry<File, String> entry(File file, String hash) {
		return new SimpleEntry<File, String>(file, hash);
	}
	

	@Override
	public void close() throws IOException {
		scanner.close();
	}
	

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
	
	
	public static class IllegalSyntaxException extends RuntimeException {
		
		public IllegalSyntaxException(int lineNumber, String line) {
			this(String.format("Illegal syntax in line %d: %s", lineNumber, line));
		}
		

		public IllegalSyntaxException(String message) {
			super(message);
		}
		
	}
	
}
