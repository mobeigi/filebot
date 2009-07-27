
package net.sourceforge.filebot.ui.panel.rename;


import static net.sourceforge.filebot.Settings.*;
import static net.sourceforge.filebot.ui.panel.rename.History.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.filebot.ui.panel.rename.History.Element;


final class HistorySpooler {
	
	private static final HistorySpooler instance = new HistorySpooler();
	

	public static HistorySpooler getInstance() {
		return instance;
	}
	

	private final File file = new File(getApplicationFolder(), "history.xml");
	
	private final History sessionHistory = new History();
	

	public synchronized History getCompleteHistory() {
		History history = new History();
		
		// add persistent history
		if (file.exists()) {
			try {
				history.addAll(importHistory(file).sequences());
			} catch (IOException e) {
				Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Failed to load history", e);
			}
		}
		
		// add session history
		history.addAll(sessionHistory.sequences());
		
		return history;
	}
	

	public synchronized void append(Iterable<Entry<File, String>> elements) {
		List<Element> sequence = new ArrayList<Element>();
		
		for (Entry<File, String> element : elements) {
			sequence.add(new Element(element.getKey().getName(), element.getValue(), element.getKey().getParentFile()));
		}
		
		// append to session history
		sessionHistory.add(sequence);
	}
	

	public synchronized void commit(History history) {
		try {
			exportHistory(history, file);
			
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
