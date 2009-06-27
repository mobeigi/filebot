
package net.sourceforge.filebot.subtitle;


import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;


public class SubViewerReader extends SubtitleReader {
	
	private final DateFormat timeFormat = new SubtitleTimeFormat();
	

	public SubViewerReader(Scanner scanner) {
		super(scanner);
	}
	

	@Override
	protected SubtitleElement readNext() throws Exception {
		// element starts with interval (e.g. 00:42:16.33,00:42:19.39)
		String[] interval = scanner.nextLine().split(",", 2);
		
		if (interval.length < 2 || interval[0].startsWith("["))
			return null;
		
		long t1 = timeFormat.parse(interval[0]).getTime();
		long t2 = timeFormat.parse(interval[1]).getTime();
		
		// append subtitle line
		List<String> lines = new ArrayList<String>(2);
		
		for (String text : scanner.nextLine().split(Pattern.quote("[br]"))) {
			lines.add(text);
		}
		
		return new SubtitleElement(t1, t2, join(lines, "\n"));
	}
	
}
