
package net.sourceforge.filebot.subtitle;


import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;


public abstract class SubtitleReader implements Iterator<SubtitleElement>, Closeable {
	
	protected final Scanner scanner;
	protected SubtitleElement current;
	

	public SubtitleReader(Readable source) {
		this.scanner = new Scanner(source);
	}
	

	protected abstract SubtitleElement readNext() throws Exception;
	

	@Override
	public boolean hasNext() {
		// find next element
		while (current == null && scanner.hasNextLine()) {
			try {
				current = readNext();
			} catch (Exception e) {
				// log and ignore
				Logger.getLogger(getClass().getName()).log(Level.WARNING, "Illegal input: " + e.getMessage());
			}
		}
		
		return current != null;
	}
	

	@Override
	public SubtitleElement next() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		
		try {
			return current;
		} finally {
			current = null;
		}
	}
	

	@Override
	public void close() throws IOException {
		scanner.close();
	}
	

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
	
}
