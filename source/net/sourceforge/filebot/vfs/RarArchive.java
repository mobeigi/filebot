
package net.sourceforge.filebot.vfs;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.tuned.ByteBufferOutputStream;
import de.innosystec.unrar.Archive;
import de.innosystec.unrar.exception.RarException;
import de.innosystec.unrar.rarfile.FileHeader;


public class RarArchive implements Iterable<MemoryFile> {
	
	private final ByteBuffer data;
	

	public RarArchive(ByteBuffer data) {
		this.data = data.duplicate();
	}
	

	@Override
	public Iterator<MemoryFile> iterator() {
		try {
			// extract rar archives one at a time, because of JUnRar memory problems
			synchronized (RarArchive.class) {
				return extract().iterator();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	

	public List<MemoryFile> extract() throws IOException {
		List<MemoryFile> vfs = new ArrayList<MemoryFile>();
		
		try {
			Archive rar = new Archive(data.duplicate());
			
			for (FileHeader header : rar.getFileHeaders()) {
				// ignore directory entries
				if (header.isDirectory()) {
					continue;
				}
				
				ByteBufferOutputStream buffer = new ByteBufferOutputStream(header.getDataSize());
				
				try {
					// write contents to buffer
					rar.extractFile(header, buffer);
					
					// add memory file
					vfs.add(new MemoryFile(header.getFileNameString(), buffer.getByteBuffer()));
				} catch (OutOfMemoryError e) {
					// ignore, there seems to be bug with JUnRar allocating lots of memory for no apparent reason
					// @see https://sourceforge.net/forum/forum.php?thread_id=2773018&forum_id=706772
					Logger.getLogger(getClass().getName()).log(Level.WARNING, "Failed to extract " + header.getFileNameString(), e);
				}
			}
		} catch (RarException e) {
			throw new IOException(e);
		}
		
		return vfs;
	}
	
}
