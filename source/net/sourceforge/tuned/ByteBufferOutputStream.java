
package net.sourceforge.tuned;


import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;


public class ByteBufferOutputStream extends OutputStream {
	
	private ByteBuffer buffer;
	
	private final float loadFactor;
	
	
	public ByteBufferOutputStream(int initialCapacity) {
		this(initialCapacity, 1.0f);
	}
	

	public ByteBufferOutputStream(int initialCapacity, float loadFactor) {
		if (initialCapacity <= 0)
			throw new IllegalArgumentException("initialCapacity must be greater than 0");
		
		if (loadFactor <= 0 || Float.isNaN(loadFactor))
			throw new IllegalArgumentException("loadFactor must be greater than 0");
		
		this.buffer = ByteBuffer.allocate(initialCapacity);
		this.loadFactor = loadFactor;
	}
	

	@Override
	public synchronized void write(int b) throws IOException {
		ensureCapacity(buffer.position() + 1);
		buffer.put((byte) b);
	}
	

	@Override
	public synchronized void write(byte[] src) throws IOException {
		ensureCapacity(buffer.position() + src.length);
		buffer.put(src);
	}
	

	@Override
	public synchronized void write(byte[] src, int offset, int length) throws IOException {
		ensureCapacity(buffer.position() + length);
		buffer.put(src, offset, length);
	}
	

	public synchronized void ensureCapacity(int minCapacity) {
		if (minCapacity <= buffer.capacity())
			return;
		
		// calculate new buffer size with load factor
		int newCapacity = (int) (buffer.capacity() * (1 + loadFactor));
		
		// ensure minCapacity
		if (newCapacity < minCapacity)
			newCapacity = minCapacity;
		
		// create new buffer with increased capacity
		ByteBuffer newBuffer = ByteBuffer.allocate(newCapacity);
		
		// copy current data to new buffer
		buffer.flip();
		newBuffer.put(buffer);
		
		buffer = newBuffer;
	}
	

	public synchronized ByteBuffer getByteBuffer() {
		ByteBuffer result = buffer.duplicate();
		
		// flip buffer so it can be read
		result.flip();
		
		return result;
	}
	

	public synchronized int transferFrom(ReadableByteChannel channel) throws IOException {
		// make sure buffer is not at its limit
		ensureCapacity(buffer.position() + 1);
		
		return channel.read(buffer);
	}
	

	public synchronized int position() {
		return buffer.position();
	}
	

	public synchronized int capacity() {
		return buffer.capacity();
	}
	

	public synchronized void rewind() {
		buffer.rewind();
	}
	
}
