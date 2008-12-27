
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
		if (buffer.remaining() <= 0)
			return -1;
		
		return buffer.get();
	}
	

	@Override
	public synchronized int read(byte[] b, int off, int len) throws IOException {
		if (buffer.remaining() <= 0)
			return -1;
		
		int length = Math.min(len, buffer.remaining());
		
		buffer.get(b, off, length);
		
		return length;
	}
	

	@Override
	public synchronized int available() throws IOException {
		return buffer.remaining();
	}
	

	@Override
	public boolean markSupported() {
		return true;
	}
	

	@Override
	public synchronized void mark(int readlimit) {
		buffer.mark();
	}
	

	@Override
	public synchronized void reset() throws IOException {
		buffer.reset();
	}
	
}
