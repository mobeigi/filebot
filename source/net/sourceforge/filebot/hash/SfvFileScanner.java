
package net.sourceforge.filebot.hash;


import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SfvFileScanner extends VerificationFileScanner {
	
	public SfvFileScanner(File file) throws FileNotFoundException {
		super(file);
	}
	

	public SfvFileScanner(Scanner scanner) {
		super(scanner);
	}
	
	/**
	 * Pattern used to parse the lines of a sfv file.
	 * 
	 * <pre>
	 * Sample:
	 * folder/file.txt 970E4EF1
	 * |  Group 1    | | Gr.2 |
	 * </pre>
	 */
	private final Pattern pattern = Pattern.compile("(.+)\\s+(\\p{XDigit}{8})");
	
	
	@Override
	protected Entry<File, String> parseLine(String line) throws IllegalSyntaxException {
		Matcher matcher = pattern.matcher(line);
		
		if (!matcher.matches())
			throw new IllegalSyntaxException(getLineNumber(), line);
		
		return entry(new File(matcher.group(1)), matcher.group(2));
	}
	
}
