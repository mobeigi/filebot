
package net.sourceforge.filebot.subtitle;


import static net.sourceforge.tuned.StringUtilities.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;


public class SubRipReader extends SubtitleReader {
	
	private final DateFormat timeFormat;
	

	public SubRipReader(Readable source) {
		super(source);
		
		// format used to parse time stamps (e.g. 00:02:26,407 --> 00:02:31,356)
		timeFormat = new SimpleDateFormat("HH:mm:ss,SSS", Locale.ROOT);
		timeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	

	@Override
	protected SubtitleElement readNext() throws Exception {
		String number = scanner.nextLine();
		
		// ignore illegal lines
		if (!number.matches("\\d+"))
			return null;
		
		String[] interval = scanner.nextLine().split("-->", 2);
		
		// ignore illegal lines
		if (interval.length < 2)
			return null;
		
		long t1 = timeFormat.parse(interval[0].trim()).getTime();
		long t2 = timeFormat.parse(interval[1].trim()).getTime();
		
		List<String> lines = new ArrayList<String>(2);
		
		// read all lines until the next empty line
		for (String line = scanner.nextLine(); line.length() > 0; line = scanner.hasNextLine() ? scanner.nextLine() : "") {
			lines.add(line);
		}
		
		return new SubtitleElement(t1, t2, join(lines, "\n"));
	}
	
}
