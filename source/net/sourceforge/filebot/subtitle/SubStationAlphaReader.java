
package net.sourceforge.filebot.subtitle;


import static net.sourceforge.tuned.StringUtilities.*;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.InputMismatchException;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;


public class SubStationAlphaReader extends SubtitleReader {
	
	private final DateFormat timeFormat = new SubtitleTimeFormat();
	private final Pattern newline = Pattern.compile(Pattern.quote("\\n"), Pattern.CASE_INSENSITIVE);
	
	private Map<EventProperty, Integer> format;
	

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
		format = new EnumMap<EventProperty, Integer>(EventProperty.class);
		
		for (int i = 0; i < columns.length; i++) {
			try {
				format.put(EventProperty.valueOf(columns[i].trim()), i);
			} catch (IllegalArgumentException e) {
				// ignore
			}
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
		String[] values = event[1].split(",", format.size());
		
		long start = timeFormat.parse(values[format.get(EventProperty.Start)]).getTime();
		long end = timeFormat.parse(values[format.get(EventProperty.End)]).getTime();
		String text = values[format.get(EventProperty.Text)].trim();
		
		// translate "\\n" to new lines 
		String[] lines = newline.split(text);
		
		return new SubtitleElement(start, end, join(lines, "\n"));
	}
	

	private enum EventProperty {
		Start,
		End,
		Text
	}
	
}
