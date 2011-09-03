
package net.sourceforge.filebot.ui.panel.subtitle;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.ibm.icu.text.CharsetDetector;

import net.sourceforge.filebot.subtitle.SubtitleElement;
import net.sourceforge.filebot.subtitle.SubtitleFormat;
import net.sourceforge.filebot.subtitle.SubtitleReader;
import net.sourceforge.tuned.ByteBufferInputStream;


final class SubtitleUtilities {
	
	/**
	 * Detect charset and parse subtitle file even if extension is invalid
	 */
	public static List<SubtitleElement> decode(MemoryFile file) throws IOException {
		// detect charset and read text content 
		CharsetDetector detector = new CharsetDetector();
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
