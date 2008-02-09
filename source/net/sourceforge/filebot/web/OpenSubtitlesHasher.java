
package net.sourceforge.filebot.web;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
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
	
	/**
	 * Size of the checksum in bytes (64 Bit)
	 */
	private static final int HASH_SIZE = 8;
	
	
	public static String computeHash(File file) throws IOException {
		long size = file.length();
		long chunkSizeForFile = Math.min(HASH_CHUNK_SIZE, size);
		
		FileChannel fileChannel = new FileInputStream(file).getChannel();
		
		BigInteger head = computeHashForChunk(fileChannel, 0, chunkSizeForFile);
		BigInteger tail = computeHashForChunk(fileChannel, Math.max(size - HASH_CHUNK_SIZE, 0), chunkSizeForFile);
		
		fileChannel.close();
		
		// size + head + tail
		BigInteger bigHash = BigInteger.valueOf(size).add(head.add(tail));
		
		byte[] hash = getTrailingBytes(bigHash.toByteArray(), HASH_SIZE);
		
		return String.format("%0" + HASH_SIZE * 2 + "x", new BigInteger(1, hash));
	}
	

	private static BigInteger computeHashForChunk(FileChannel fileChannel, long start, long size) throws IOException {
		MappedByteBuffer buffer = fileChannel.map(MapMode.READ_ONLY, start, size);
		
		BigInteger bigHash = BigInteger.ZERO;
		byte[] bytes = new byte[HASH_SIZE];
		
		while (buffer.hasRemaining()) {
			buffer.get(bytes, 0, Math.min(HASH_SIZE, buffer.remaining()));
			
			// BigInteger expects a big-endian byte-order, so we reverse the byte array
			bigHash = bigHash.add(new BigInteger(1, reverse(bytes)));
		}
		
		return bigHash;
	}
	

	/**
	 * copy the last n bytes to a new array
	 * 
	 * @param bytes original array
	 * @param n number of trailing bytes
	 * @return new array
	 */
	private static byte[] getTrailingBytes(byte[] src, int n) {
		int length = Math.min(src.length, n);
		
		byte[] dest = new byte[length];
		
		int offsetSrc = Math.max(src.length - n, 0);
		System.arraycopy(src, offsetSrc, dest, 0, length);
		
		return dest;
	}
	

	private static byte[] reverse(byte[] bytes) {
		byte[] reverseBytes = new byte[bytes.length];
		
		for (int forward = 0, backward = bytes.length; forward < bytes.length; ++forward)
			reverseBytes[forward] = bytes[--backward];
		
		return reverseBytes;
	}
	
}
