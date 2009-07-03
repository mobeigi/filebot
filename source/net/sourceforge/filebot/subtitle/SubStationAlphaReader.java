
package net.sourceforge.filebot.subtitle;


import static net.sourceforge.tuned.StringUtilities.*;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;


public class SubStationAlphaReader extends SubtitleReader {
	
	private final DateFormat timeFormat = new SubtitleTimeFormat();
	
	private Map<String, Integer> format;
	

	public SubStationAlphaReader(Scanner scanner) {
		super(scanner);
	}
	

	private void readFormat() throws Exception {
		// read format line (e.g. Format: Marked, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text)
		String[] event = scanner.nextLine().split(":", 2);
		
		// sanity check
		if (!event[0].equals("Format"))
			throw new InputMismatchException("Illegal format header: " + Arrays.toString(event));
		
		String[] columns = event[1].split(",");
		
		// map column name to column index
		format = new HashMap<String, Integer>(columns.length);
		
		for (int i = 0; i < columns.length; i++) {
			format.put(columns[i].trim(), i);
		}
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
		String[] row = event[1].split(",", format.size());
		
		long start = timeFormat.parse(row[format.get("Start")]).getTime();
		long end = timeFormat.parse(row[format.get("End")]).getTime();
		String text = row[format.get("Text")].trim();
		
		// translate "\\n" to new lines 
		String[] lines = Pattern.compile(Pattern.quote("\\N"), Pattern.CASE_INSENSITIVE).split(text);
		
		return new SubtitleElement(start, end, join(lines, "\n"));
	}
	
}
