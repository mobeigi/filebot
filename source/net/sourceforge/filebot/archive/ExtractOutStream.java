
package net.sourceforge.filebot.archive;


import java.io.*;

import net.sf.sevenzipjbinding.*;


class ExtractOutStream implements ISequentialOutStream, Closeable {
	
	private OutputStream out;
	
	
	public ExtractOutStream(OutputStream out) {
		this.out = out;
	}
	
	
	@Override
	public int write(byte[] data) throws SevenZipException {
		try {
			out.write(data);
		} catch (IOException e) {
			throw new SevenZipException(e);
		}
		return data.length; // return amount of proceed data
	}
	
	
	@Override
	public void close() throws IOException {
		out.close();
	}
	
}
