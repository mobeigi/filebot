
package net.sourceforge.filebot.ui.panel.subtitle;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.sourceforge.filebot.subtitle.SubtitleElement;
import net.sourceforge.filebot.subtitle.SubtitleFormat;
import net.sourceforge.filebot.subtitle.SubtitleReader;
import net.sourceforge.tuned.ByteBufferInputStream;


final class SubtitleUtilities {
	
	/**
	 * Decode subtitle file even if extension is invalid.
	 */
	public static List<SubtitleElement> decode(MemoryFile file) throws IOException {
		LinkedList<SubtitleFormat> priorityList = new LinkedList<SubtitleFormat>();
		
		// gather all formats, put likely formats first
		for (SubtitleFormat format : SubtitleFormat.values()) {
			if (format.getFilter().accept(file.getName())) {
				priorityList.addFirst(format);
			} else {
				priorityList.addLast(format);
			}
		}
		
		// decode subtitle file with the first reader that seems to work
		for (SubtitleFormat format : priorityList) {
			InputStream data = new ByteBufferInputStream(file.getData());
			SubtitleReader reader = format.newReader(new InputStreamReader(data, "UTF-8"));
			
			try {
				if (reader.hasNext()) {
					// correct format found
					List<SubtitleElement> list = new ArrayList<SubtitleElement>(500);
					
					// read subtitle file
					while (reader.hasNext()) {
						list.add(reader.next());
					}
					
					return list;
				}
			} finally {
				reader.close();
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
