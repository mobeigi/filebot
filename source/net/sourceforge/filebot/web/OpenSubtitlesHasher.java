
package net.sourceforge.filebot.web;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;


/**
 * Hash code is based on Media Player Classic. In natural language it calculates: size + 64bit
 * checksum of the first and last 64k (even if they overlap because the file is smaller than
 * 128k).
 */
public class OpenSubtitlesHasher {
	
	/**
	 * Size of the chunks that will be hashed in bytes (64 KB)
	 */
	private static final int HASH_CHUNK_SIZE = 64 * 1024;
	
	
	public static String computeHash(File file) throws IOException {
		long size = file.length();
		long chunkSizeForFile = Math.min(HASH_CHUNK_SIZE, size);
		
		FileChannel fileChannel = new FileInputStream(file).getChannel();
		
		try {
			long head = computeHashForChunk(fileChannel, 0, chunkSizeForFile);
			long tail = computeHashForChunk(fileChannel, Math.max(size - HASH_CHUNK_SIZE, 0), chunkSizeForFile);
			
			return String.format("%016x", size + head + tail);
		} finally {
			fileChannel.close();
		}
	}
	

	private static long computeHashForChunk(FileChannel fileChannel, long start, long size) throws IOException {
		MappedByteBuffer byteBuffer = fileChannel.map(MapMode.READ_ONLY, start, size);
		
		LongBuffer longBuffer = byteBuffer.order(ByteOrder.LITTLE_ENDIAN).asLongBuffer();
		long hash = 0;
		
		while (longBuffer.hasRemaining()) {
			hash += longBuffer.get();
		}
		
		return hash;
	}
	
}
