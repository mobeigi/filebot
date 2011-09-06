
package net.sourceforge.filebot.ui.panel.subtitle;


import static java.lang.Math.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.ibm.icu.text.CharsetDetector;

import net.sourceforge.filebot.subtitle.SubRipWriter;
import net.sourceforge.filebot.subtitle.SubtitleElement;
import net.sourceforge.filebot.subtitle.SubtitleFormat;
import net.sourceforge.filebot.subtitle.SubtitleReader;
import net.sourceforge.tuned.ByteBufferInputStream;


final class SubtitleUtilities {
	
	/**
	 * Detect charset and parse subtitle file even if extension is invalid
	 */
	public static List<SubtitleElement> decodeSubtitles(MemoryFile file) throws IOException {
		// detect charset and read text content 
		CharsetDetector detector = new CharsetDetector();
		detector.setDeclaredEncoding("UTF-8");
		detector.enableInputFilter(true);
		
		detector.setText(new ByteBufferInputStream(file.getData()));
		String textfile = detector.detect().getString();
		
		// gather all formats, put likely formats first
		LinkedList<SubtitleFormat> priorityList = new LinkedList<SubtitleFormat>();
		
		for (SubtitleFormat format : SubtitleFormat.values()) {
			if (format.getFilter().accept(file.getName())) {
				priorityList.addFirst(format);
			} else {
				priorityList.addLast(format);
			}
		}
		
		// decode subtitle file with the first reader that seems to work
		for (SubtitleFormat format : priorityList) {
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
	

	/**
	 * Write a subtitle file to disk
	 */
	public static void exportSubtitles(List<SubtitleElement> data, File destination, Charset encoding, SubtitleFormat format, long timingOffset) throws IOException {
		if (format != SubtitleFormat.SubRip)
			throw new IllegalArgumentException("Format not supported");
		
		StringBuilder buffer = new StringBuilder(4 * 1024);
		SubRipWriter out = new SubRipWriter(buffer);
		
		for (SubtitleElement it : data) {
			if (timingOffset != 0)
				it = new SubtitleElement(max(0, it.getStart() + timingOffset), max(0, it.getEnd() + timingOffset), it.getText());
			
			out.write(it);
		}
		
		// write to file
		write(encoding.encode(CharBuffer.wrap(buffer)), destination);
	}
	

	/**
	 * Write {@link ByteBuffer} to {@link File}.
	 */
	public static void write(ByteBuffer data, File destination) throws IOException {
		FileChannel fileChannel = new FileOutputStream(destination).getChannel();
		
		try {
			fileChannel.write(data);
		} finally {
			fileChannel.close();
		}
	}
	

	/**
	 * Dummy constructor to prevent instantiation.
	 */
	private SubtitleUtilities() {
		throw new UnsupportedOperationException();
	}
	
}
