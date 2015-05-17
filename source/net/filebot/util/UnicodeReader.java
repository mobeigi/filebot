package net.filebot.util;

import java.io.*;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class UnicodeReader extends Reader {

	private static final int BOM_SIZE = 4;

	private InputStreamReader reader = null;

	public UnicodeReader(InputStream stream) throws IOException {
		if (!stream.markSupported())
			throw new IllegalArgumentException("stream must support mark");

		stream.mark(BOM_SIZE);
		byte bom[] = new byte[BOM_SIZE];
		stream.read(bom, 0, bom.length);

		Charset charset = StandardCharsets.UTF_8;
		int skip = 0;

		if ((bom[0] == (byte) 0xEF) && (bom[1] == (byte) 0xBB) && (bom[2] == (byte) 0xBF)) {
			charset = StandardCharsets.UTF_8;
			skip = 3;
		} else if ((bom[0] == (byte) 0xFE) && (bom[1] == (byte) 0xFF)) {
			charset = StandardCharsets.UTF_16BE;
			skip = 2;
		} else if ((bom[0] == (byte) 0xFF) && (bom[1] == (byte) 0xFE)) {
			charset = StandardCharsets.UTF_16LE;
			skip = 2;
		} else if ((bom[0] == (byte) 0x00) && (bom[1] == (byte) 0x00) && (bom[2] == (byte) 0xFE) && (bom[3] == (byte) 0xFF)) {
			charset = Charset.forName("UTF-32BE");
			skip = 4;
		} else if ((bom[0] == (byte) 0xFF) && (bom[1] == (byte) 0xFE) && (bom[2] == (byte) 0x00) && (bom[3] == (byte) 0x00)) {
			charset = Charset.forName("UTF-32LE");
			skip = 4;
		}

		stream.reset();
		stream.skip(skip);

		// initialize reader
		reader = new InputStreamReader(stream, charset);
	}

	public int hashCode() {
		return reader.hashCode();
	}

	public int read(CharBuffer target) throws IOException {
		return reader.read(target);
	}

	public boolean equals(Object obj) {
		return reader.equals(obj);
	}

	public int read(char[] cbuf) throws IOException {
		return reader.read(cbuf);
	}

	public String getEncoding() {
		return reader.getEncoding();
	}

	public int read() throws IOException {
		return reader.read();
	}

	public int read(char[] cbuf, int offset, int length) throws IOException {
		return reader.read(cbuf, offset, length);
	}

	public long skip(long n) throws IOException {
		return reader.skip(n);
	}

	public boolean ready() throws IOException {
		return reader.ready();
	}

	public void close() throws IOException {
		reader.close();
	}

	public boolean markSupported() {
		return reader.markSupported();
	}

	public void mark(int readAheadLimit) throws IOException {
		reader.mark(readAheadLimit);
	}

	public void reset() throws IOException {
		reader.reset();
	}

}
