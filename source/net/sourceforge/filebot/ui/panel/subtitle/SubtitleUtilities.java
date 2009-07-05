
package net.sourceforge.filebot.ui.panel.subtitle;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import net.sourceforge.filebot.subtitle.SubtitleElement;
import net.sourceforge.filebot.subtitle.SubtitleFormat;
import net.sourceforge.filebot.subtitle.SubtitleReader;
import net.sourceforge.tuned.ByteBufferInputStream;


final class SubtitleUtilities {
	
	public static List<SubtitleElement> decode(MemoryFile file) throws IOException {
		Deque<SubtitleFormat> priorityList = new ArrayDeque<SubtitleFormat>();
		
		// gather all formats, put likely formats first
		for (SubtitleFormat format : SubtitleFormat.values()) {
			if (format.filter().accept(file.getName())) {
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
	

	public static void write(MemoryFile source, File destination) throws IOException {
		FileChannel fileChannel = new FileOutputStream(destination).getChannel();
		
		try {
			fileChannel.write(source.getData());
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
