
package net.sourceforge.filebot;


import static net.sourceforge.filebot.Settings.*;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.filebot.History.Element;
import net.sourceforge.tuned.ByteBufferInputStream;
import net.sourceforge.tuned.ByteBufferOutputStream;


public final class HistorySpooler {
	
	private static final HistorySpooler instance = new HistorySpooler();
	
	
	public static HistorySpooler getInstance() {
		return instance;
	}
	
	private File persistentHistoryFile = new File(getApplicationFolder(), "history.xml");
	private int persistentHistoryTotalSize = -1;
	private boolean persistentHistoryEnabled = true;
	
	private History sessionHistory = new History();
	
	
	public synchronized History getCompleteHistory() throws IOException {
		if (!persistentHistoryEnabled || persistentHistoryFile.length() <= 0) {
			return new History();
		}
		
		RandomAccessFile f = new RandomAccessFile(persistentHistoryFile, "rw");
		FileChannel channel = f.getChannel();
		FileLock lock = channel.lock();
		try {
			ByteBufferOutputStream data = new ByteBufferOutputStream(f.length());
			data.transferFully(channel);
			
			History history = History.importHistory(new ByteBufferInputStream(data.getByteBuffer()));
			history.addAll(sessionHistory.sequences());
			return history;
		} finally {
			lock.release();
			channel.close();
			f.close();
		}
	}
	
	
	public synchronized void commit() {
		if (!persistentHistoryEnabled || sessionHistory.sequences().isEmpty()) {
			return;
		}
		
		try {
			if (persistentHistoryFile.length() <= 0) {
				persistentHistoryFile.createNewFile();
			}
			RandomAccessFile f = new RandomAccessFile(persistentHistoryFile, "rw");
			FileChannel channel = f.getChannel();
			FileLock lock = channel.lock();
			try {
				ByteBufferOutputStream data = new ByteBufferOutputStream(f.length());
				int read = data.transferFully(channel);
				
				History history = read > 0 ? History.importHistory(new ByteBufferInputStream(data.getByteBuffer())) : new History();
				history.addAll(sessionHistory.sequences());
				
				data.rewind();
				History.exportHistory(history, data);
				
				channel.position(0);
				channel.write(data.getByteBuffer());
				
				sessionHistory.clear();
				persistentHistoryTotalSize = history.totalSize();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				lock.release();
				channel.close();
				f.close();
			}
		} catch (Exception e) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Failed to write rename history.", e);
		}
	}
	
	
	public synchronized void append(Iterable<Entry<File, File>> elements) {
		List<Element> sequence = new ArrayList<Element>();
		
		for (Entry<File, File> element : elements) {
			sequence.add(new Element(element.getKey().getName(), element.getValue().getPath(), element.getKey().getParentFile()));
		}
		
		// append to session history
		if (sequence.size() > 0) {
			sessionHistory.add(sequence);
		}
	}
	
	
	public History getSessionHistory() {
		return sessionHistory;
	}
	
	
	public int getPersistentHistoryTotalSize() {
		return persistentHistoryTotalSize;
	}
	
	
	public void setPersistentHistoryEnabled(boolean persistentHistoryEnabled) {
		this.persistentHistoryEnabled = persistentHistoryEnabled;
	}
	
}
