
package net.sourceforge.filebot.ui.panel.subtitle;


import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
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
	

	public Map<File, ByteBuffer> extract() throws IOException {
		Map<File, ByteBuffer> vfs = new LinkedHashMap<File, ByteBuffer>();
		
		// read first zip entry
		ZipInputStream zipInputStream = new ZipInputStream(new ByteBufferInputStream(data.duplicate()));
		ZipEntry zipEntry;
		
		try {
			while ((zipEntry = zipInputStream.getNextEntry()) != null) {
				// ignore directory entries
				if (zipEntry.isDirectory()) {
					continue;
				}
				
				ByteBufferOutputStream buffer = new ByteBufferOutputStream((int) zipEntry.getSize());
				
				// write contents to buffer
				buffer.transferFully(zipInputStream);
				
				// add memory file
				vfs.put(new File(zipEntry.getName()), buffer.getByteBuffer());
			}
		} finally {
			zipInputStream.close();
		}
		
		return vfs;
	}
}
