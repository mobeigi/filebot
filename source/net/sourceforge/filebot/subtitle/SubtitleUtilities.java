
package net.sourceforge.filebot.subtitle;


import static java.lang.Math.*;
import static net.sourceforge.tuned.FileUtilities.*;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.sourceforge.filebot.vfs.MemoryFile;


public final class SubtitleUtilities {
	
	/**
	 * Detect charset and parse subtitle file even if extension is invalid
	 */
	public static List<SubtitleElement> decodeSubtitles(MemoryFile file) throws IOException {
		// gather all formats, put likely formats first
		LinkedList<SubtitleFormat> likelyFormats = new LinkedList<SubtitleFormat>();
		
		for (SubtitleFormat format : SubtitleFormat.values()) {
			if (format.getFilter().accept(file.getName()))
				likelyFormats.addFirst(format);
			else
				likelyFormats.addLast(format);
		}
		
		// decode bytes
		String textfile = getText(file.getData());
		
		// decode subtitle file with the first reader that seems to work
		for (SubtitleFormat format : likelyFormats) {
			// reset reader to position 0
			SubtitleReader parser = format.newReader(new StringReader(textfile));
			
			if (parser.hasNext()) {
				// correct format found
				List<SubtitleElement> list = new ArrayList<SubtitleElement>(500);
				
				// read subtitle file
				while (parser.hasNext()) {
					list.add(parser.next());
				}
				
				return list;
			}
		}
		
		// unsupported subtitle format
		throw new IOException("Cannot read subtitle format");
	}
	

	public static ByteBuffer exportSubtitles(MemoryFile data, SubtitleFormat outputFormat, long outputTimingOffset, Charset outputEncoding) throws IOException {
		if (outputFormat != null && outputFormat != SubtitleFormat.SubRip) {
			throw new IllegalArgumentException("Format not supported");
		}
		
		// convert to target format and target encoding
		if (outputFormat == SubtitleFormat.SubRip) {
			// output buffer
			StringBuilder buffer = new StringBuilder(4 * 1024);
			SubRipWriter out = new SubRipWriter(buffer);
			
			for (SubtitleElement it : decodeSubtitles(data)) {
				if (outputTimingOffset != 0)
					it = new SubtitleElement(max(0, it.getStart() + outputTimingOffset), max(0, it.getEnd() + outputTimingOffset), it.getText());
				
				out.write(it);
			}
			
			return outputEncoding.encode(CharBuffer.wrap(buffer));
		}
		
		// only change encoding
		return outputEncoding.encode(getText(data.getData()));
	}
	

	public static boolean isDerived(String subtitle, File video) {
		return isDerived(subtitle, getName(video));
	}
	

	public static boolean isDerived(String derivate, String base) {
		if (derivate.equalsIgnoreCase(base))
			return true;
		
		while (getExtension(derivate) != null) {
			derivate = getNameWithoutExtension(derivate);
			
			if (derivate.equalsIgnoreCase(base))
				return true;
		}
		
		return false;
	}
	

	public static SubtitleFormat getSubtitleFormat(File file) {
		for (SubtitleFormat it : SubtitleFormat.values()) {
			if (it.getFilter().accept(file))
				return it;
		}
		
		return null;
	}
	

	public static SubtitleFormat getSubtitleFormatByName(String name) {
		for (SubtitleFormat it : SubtitleFormat.values()) {
			// check by name
			if (it.name().equalsIgnoreCase(name))
				return it;
			
			// check by extension
			if (it.getFilter().acceptExtension(name))
				return it;
		}
		
		return null;
	}
	

	/**
	 * Dummy constructor to prevent instantiation.
	 */
	private SubtitleUtilities() {
		throw new UnsupportedOperationException();
	}
	
}
