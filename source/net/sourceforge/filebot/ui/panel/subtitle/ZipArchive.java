
package net.sourceforge.filebot.ui.panel.subtitle;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import net.sourceforge.tuned.ByteBufferInputStream;
import net.sourceforge.tuned.ByteBufferOutputStream;


class ZipArchive implements Archive {
	
	private final ByteBuffer data;
	

	public ZipArchive(ByteBuffer data) {
		this.data = data.duplicate();
	}
	

	public Map<String, ByteBuffer> extract() throws IOException {
		Map<String, ByteBuffer> vfs = new LinkedHashMap<String, ByteBuffer>();
		
		// read first zip entry
		ZipInputStream zipInputStream = new ZipInputStream(new ByteBufferInputStream(data.duplicate()));
		ZipEntry zipEntry;
		
		try {
			while ((zipEntry = zipInputStream.getNextEntry()) != null) {
				ByteBufferOutputStream buffer = new ByteBufferOutputStream((int) zipEntry.getSize());
				ReadableByteChannel fileChannel = Channels.newChannel(zipInputStream);
				
				// write contents to buffer
				while (buffer.transferFrom(fileChannel) >= 0);
				
				// add memory file
				vfs.put(zipEntry.getName(), buffer.getByteBuffer());
			}
		} finally {
			zipInputStream.close();
		}
		
		return vfs;
	}
}
