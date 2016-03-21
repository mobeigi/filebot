
package net.filebot.subtitle;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.List;
import java.util.regex.Pattern;

public class SubStationAlphaReader extends SubtitleReader {

	private final DateFormat timeFormat = new SubtitleTimeFormat();
	private final Pattern newline = Pattern.compile(Pattern.quote("\\n"), Pattern.CASE_INSENSITIVE);
	private final Pattern tag = Pattern.compile("[{]\\\\[^}]+[}]");

	private String[] format;
	private int formatIndexStart;
	private int formatIndexEnd;
	private int formatIndexText;

	public SubStationAlphaReader(Readable source) {
		super(source);
	}

	@Override
	public String getFormatName() {
		return "SubStationAlpha";
	}

	private void readFormat() throws Exception {
		// read format line (e.g. Format: Marked, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text)
		String[] event = scanner.nextLine().split(":", 2);

		// sanity check
		if (!event[0].equals("Format"))
			throw new InputMismatchException("Illegal format header: " + Arrays.toString(event));

		// read columns
		format = event[1].split(",");

		// normalize column names
		for (int i = 0; i < format.length; i++) {
			format[i] = format[i].trim().toLowerCase();
		}

		List<String> lookup = Arrays.asList(format);
		formatIndexStart = lookup.indexOf("start");
		formatIndexEnd = lookup.indexOf("end");
		formatIndexText = lookup.indexOf("text");
	}

	@Override
	public SubtitleElement readNext() throws Exception {
		if (format == null) {
			// move to [Events] sections
			boolean found = false;

			while (!found && scanner.hasNextLine()) {
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
		String[] values = event[1].split(",", format.length);

		long start = timeFormat.parse(values[formatIndexStart].trim()).getTime();
		long end = timeFormat.parse(values[formatIndexEnd].trim()).getTime();
		String text = values[formatIndexText].trim();

		return new SubtitleElement(start, end, resolve(text));
	}

	protected String resolve(String text) {
		// remove tags
		text = tag.matcher(text).replaceAll("");

		// resolve line breaks
		return newline.matcher(text).replaceAll("\n");
	}

}
