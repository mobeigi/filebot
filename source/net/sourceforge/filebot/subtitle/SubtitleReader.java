
package net.sourceforge.filebot.subtitle;


import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;


public abstract class SubtitleReader implements Iterator<SubtitleElement>, Closeable {
	
	protected final Scanner scanner;
	
	protected SubtitleElement current;
	

	public SubtitleReader(File file) throws FileNotFoundException {
		// don't use new Scanner(File) because of BUG 6368019 (http://bugs.sun.com/view_bug.do?bug_id=6368019)
		this(new Scanner(new FileInputStream(file), "UTF-8"));
	}
	

	public SubtitleReader(Scanner scanner) {
		this.scanner = scanner;
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
				Logger.getLogger(getClass().getName()).log(Level.WARNING, e.toString(), e);
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
	

	protected String join(Object[] values, String delimiter) {
		StringBuilder sb = new StringBuilder();
		
		for (int i = 0; i < values.length; i++) {
			sb.append(values[i]);
			
			if (i < values.length - 1) {
				sb.append(delimiter);
			}
		}
		
		return sb.toString();
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
