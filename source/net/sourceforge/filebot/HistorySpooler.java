
package net.sourceforge.filebot;


import static net.sourceforge.filebot.History.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.filebot.History.Element;


public final class HistorySpooler {
	
	private static final HistorySpooler instance = new HistorySpooler();
	
	
	public static HistorySpooler getInstance() {
		return instance;
	}
	
	private HistoryStorage persistentHistory = null;
	private History sessionHistory = new History();
	
	
	public synchronized History getCompleteHistory() {
		History history = new History();
		
		// add persistent history
		try {
			if (getPersistentHistory() != null) {
				history.addAll(getPersistentHistory().read().sequences());
			}
		} catch (IOException e) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Failed to load history", e);
		}
		
		// add session history
		history.addAll(sessionHistory.sequences());
		
		return history;
	}
	
	
	public History getSessionHistory() {
		return sessionHistory;
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
	
	
	public synchronized void commit(History history) {
		try {
			if (getPersistentHistory() != null) {
				getPersistentHistory().write(history);
			}
			
			// clear session history
			sessionHistory.clear();
		} catch (IOException e) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Failed to store history", e);
		}
	}
	
	
	public synchronized void commit() {
		// check if session history is not empty
		if (sessionHistory.sequences().size() > 0) {
			commit(getCompleteHistory());
		}
	}
	
	
	public synchronized void setPersistentHistory(HistoryStorage persistentHistory) {
		this.persistentHistory = persistentHistory;
	}
	
	
	public synchronized HistoryStorage getPersistentHistory() {
		return persistentHistory;
	}
	
	
	public static interface HistoryStorage {
		
		History read() throws IOException;
		
		
		void write(History history) throws IOException;
	}
	
	
	public static class HistoryFileStorage implements HistoryStorage {
		
		private final File file;
		
		
		public HistoryFileStorage(File file) {
			this.file = file;
		}
		
		
		@Override
		public History read() throws IOException {
			if (file.exists()) {
				return importHistory(file);
			} else {
				return new History();
			}
		}
		
		
		@Override
		public void write(History history) throws IOException {
			exportHistory(history, file);
		}
	}
	
}
