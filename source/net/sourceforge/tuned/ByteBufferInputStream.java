
package net.sourceforge.tuned;


import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;


public class ByteBufferInputStream extends InputStream {
	
	private final ByteBuffer buffer;
	
	
	public ByteBufferInputStream(ByteBuffer buffer) {
		this.buffer = buffer;
	}
	

	@Override
	public synchronized int read() throws IOException {
		return buffer.getInt();
	}
	

	@Override
	public synchronized int read(byte[] b, int off, int len) throws IOException {
		int length = Math.min(len, buffer.remaining());
		
		buffer.get(b, off, length);
		
		return length;
	}
	

	@Override
	public synchronized int available() throws IOException {
		return buffer.remaining();
	}
	
}
