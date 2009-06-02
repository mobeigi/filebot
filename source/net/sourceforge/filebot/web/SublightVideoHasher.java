
package net.sourceforge.filebot.web;


import static java.lang.Math.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.security.MessageDigest;
import java.util.Formatter;
import java.util.concurrent.TimeUnit;

import net.sourceforge.filebot.mediainfo.MediaInfo;
import net.sourceforge.filebot.mediainfo.MediaInfo.StreamKind;


/**
 * Compute special hash used by <a href="http://www.subtitles-on.net">Sublight</a> to identify video files.
 * 
 * <pre>
 * The hash is divided into 5 sections:
 * 1 byte : reserved
 * 2 bytes: video duration in seconds
 * 6 bytes: file size in bytes
 * 16 bytes: MD5 hash of the first 5 MB
 * 1 byte: control byte, sum of all other bytes
 * </pre>
 */
public final class SublightVideoHasher {
	
	
	public static String computeHash(File file) throws IOException, LinkageError {
		byte[][] hash = new byte[4][];
		
		// 1 byte = 0 (reserved)
		hash[0] = new byte[] { 0 };
		
		// 2 bytes (video duration in seconds)
		hash[1] = getTrailingBytes(getDuration(file, TimeUnit.SECONDS), 2);
		
		// 6 bytes (file size in bytes)
		hash[2] = getTrailingBytes(file.length(), 6);
		
		// 16 bytes (md5 hash of the first 5 MB)
		hash[3] = getHeadMD5(file, 5 * 1024 * 1024);
		
		// format and sum
		Formatter hex = new Formatter(new StringBuilder(52));
		byte sum = 0;
		
		for (byte[] group : hash) {
			for (byte b : group) {
				hex.format("%02x", b);
				sum += b;
			}
		}
		
		// 1 byte (control byte)
		hex.format("%02x", sum);
		
		// done
		return hex.out().toString();
	}
	

	protected static byte[] getTrailingBytes(long value, int n) {
		byte[] bytes = BigInteger.valueOf(value).toByteArray();
		
		// bytes will be initialized with 0
		byte[] trailingBytes = new byte[n];
		
		// copy the least significant n bytes to the new array
		System.arraycopy(bytes, max(0, bytes.length - n), trailingBytes, max(0, n - bytes.length), min(n, bytes.length));
		
		return trailingBytes;
	}
	

	protected static long getDuration(File file, TimeUnit unit) throws IOException, LinkageError {
		MediaInfo mediaInfo = new MediaInfo();
		
		if (!mediaInfo.open(file))
			throw new IOException("Failed to open file: " + file);
		
		// get media info
		String duration = mediaInfo.get(StreamKind.General, 0, "Duration");
		
		// close handle
		mediaInfo.close();
		
		// convert from milliseconds to given unit
		return unit.convert(Long.parseLong(duration), TimeUnit.MILLISECONDS);
	}
	

	protected static byte[] getHeadMD5(File file, long chunkSize) throws IOException {
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			
			FileChannel channel = new FileInputStream(file).getChannel();
			
			try {
				// calculate md5
				md5.update(channel.map(MapMode.READ_ONLY, 0, min(channel.size(), chunkSize)));
			} finally {
				// close channel
				channel.close();
			}
			
			return md5.digest();
		} catch (Exception e) {
			throw new IOException("Failed to calculate md5 hash", e);
		}
	}
	
}
