
package net.sourceforge.filebot.subtitle;


import java.text.DateFormat;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.List;
import java.util.regex.Pattern;


public class SubStationAlphaReader extends SubtitleReader {
	
	private final DateFormat timeFormat = new SubtitleTimeFormat();
	private final Pattern newline = Pattern.compile(Pattern.quote("\\n"), Pattern.CASE_INSENSITIVE);
	private final Pattern tag = Pattern.compile("[{]\\\\[^}]+[}]");
	private final Pattern separator = Pattern.compile("\\s*,\\s*");
	
	private String[] format;
	private int formatIndexStart;
	private int formatIndexEnd;
	private int formatIndexText;
	

	public SubStationAlphaReader(Readable source) {
		super(source);
	}
	

	private void readFormat() throws Exception {
		// read format line (e.g. Format: Marked, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text)
		String[] event = scanner.nextLine().split(":", 2);
		
		// sanity check
		if (!event[0].equals("Format"))
			throw new InputMismatchException("Illegal format header: " + Arrays.toString(event));
		
		// read columns
		format = separator.split(event[1]);
		
		List<String> lookup = Arrays.asList(format);
		formatIndexStart = lookup.indexOf("Start");
		formatIndexEnd = lookup.indexOf("End");
		formatIndexText = lookup.indexOf("Text");
	}
	

	@Override
	public SubtitleElement readNext() throws Exception {
		if (format == null) {
			// move to [Events] sections
			boolean found = false;
			
			while (!found && scanner.hasNext()) {
				found = scanner.nextLine().equals("[Events]");
			}
			
			if (!found) {
				throw new InputMismatchException("Cannot find [Events] section");
			}
			
			// read format header
			readFormat();
		}
		
		// read next dialogue line
		String[] event = scanner.nextLine().split(":", 2);
		
		// sanity check
		if (!event[0].equals("Dialogue"))
			throw new InputMismatchException("Illegal dialogue event: " + Arrays.toString(event));
		
		// extract information
		String[] values = separator.split(event[1], format.length);
		
		long start = timeFormat.parse(values[formatIndexStart]).getTime();
		long end = timeFormat.parse(values[formatIndexEnd]).getTime();
		String text = values[formatIndexText];
		
		return new SubtitleElement(start, end, resolve(text));
	}
	

	protected String resolve(String text) {
		// remove tags
		text = tag.matcher(text).replaceAll("");
		
		// resolve line breaks 
		return newline.matcher(text).replaceAll("\n");
	}
	
}
