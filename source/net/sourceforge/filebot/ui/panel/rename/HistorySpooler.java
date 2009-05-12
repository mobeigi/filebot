
package net.sourceforge.filebot.ui.panel.rename;


import java.io.File;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;


final class HistorySpooler {
	
	private static final HistorySpooler instance = new HistorySpooler();
	
	
	public static HistorySpooler getInstance() {
		return instance;
	}
	
	private final History sessionHistory = new History();
	
	private final File file = new File("history.xml");
	
	
	public synchronized History getHistory() {
		History history = new History();
		
		// add persistent history
		if (file.exists()) {
			try {
				history.load(file);
			} catch (Exception e) {
				Logger.getLogger("global").log(Level.SEVERE, "Failed to load history", e);
			}
		}
		
		// add session history
		history.add(sessionHistory);
		
		return history;
	}
	

	public synchronized void append(Collection<Entry<File, File>> elements) {
		if (elements.isEmpty())
			return;
		
		// append to session history
		sessionHistory.add(elements);
	}
	

	public synchronized void commit() {
		if (sessionHistory.size() > 0) {
			try {
				getHistory().store(file);
				
				// clear session history
				sessionHistory.clear();
			} catch (Exception e) {
				Logger.getLogger("global").log(Level.SEVERE, "Failed to store history", e);
			}
		}
	}
	

	private HistorySpooler() {
		// commit session history on shutdown
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			
			@Override
			public void run() {
				commit();
			}
		}));
	}
}
