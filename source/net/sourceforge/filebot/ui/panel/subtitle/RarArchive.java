
package net.sourceforge.filebot.ui.panel.subtitle;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

import net.sourceforge.tuned.ByteBufferOutputStream;

import de.innosystec.unrar.exception.RarException;
import de.innosystec.unrar.rarfile.FileHeader;


class RarArchive implements Archive {
	
	private final ByteBuffer data;
	

	public RarArchive(ByteBuffer data) {
		this.data = data.duplicate();
	}
	

	public Map<String, ByteBuffer> extract() throws IOException {
		Map<String, ByteBuffer> vfs = new LinkedHashMap<String, ByteBuffer>();
		
		try {
			de.innosystec.unrar.Archive rar = new de.innosystec.unrar.Archive(data.duplicate());
			
			for (FileHeader header : rar.getFileHeaders()) {
				// ignore directory entries
				if (header.isDirectory()) {
					continue;
				}
				
				ByteBufferOutputStream buffer = new ByteBufferOutputStream(header.getDataSize());
				
				// write contents to buffer
				rar.extractFile(header, buffer);
				
				// add memory file
				vfs.put(header.getFileNameString(), buffer.getByteBuffer());
			}
		} catch (RarException e) {
			throw new IOException(e);
		}
		
		return vfs;
	}
	
}
