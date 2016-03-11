package net.filebot;

import static net.filebot.Logging.*;
import static net.filebot.Settings.*;

import java.io.File;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import net.filebot.History.Element;

import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.commons.io.output.CloseShieldOutputStream;

public final class HistorySpooler {

	private static final HistorySpooler instance = new HistorySpooler();

	public static HistorySpooler getInstance() {
		return instance;
	}

	// commit session history on shutdown
	static {
		Runtime.getRuntime().addShutdownHook(new Thread() {

			@Override
			public void run() {
				HistorySpooler.getInstance().commit();
			}
		});
	}

	private File persistentHistoryFile = ApplicationFolder.AppData.resolve("history.xml");
	private int persistentHistoryTotalSize = -1;
	private boolean persistentHistoryEnabled = true;

	private History sessionHistory = new History();

	public synchronized History getCompleteHistory() throws IOException {
		if (persistentHistoryFile.length() <= 0) {
			return new History(sessionHistory.sequences());
		}

		try (FileChannel channel = FileChannel.open(persistentHistoryFile.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
			try (FileLock lock = channel.lock()) {
				History history = History.importHistory(new CloseShieldInputStream(Channels.newInputStream(channel))); // keep JAXB from closing the stream
				history.addAll(sessionHistory.sequences());
				return history;
			}
		}
	}

	public synchronized void commit() {
		if (!persistentHistoryEnabled || sessionHistory.sequences().isEmpty()) {
			return;
		}

		try {
			try (FileChannel channel = FileChannel.open(persistentHistoryFile.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
				try (FileLock lock = channel.lock()) {
					History history = new History();

					// load existing history from previous sessions
					if (channel.size() > 0) {
						try {
							channel.position(0);
							history = History.importHistory(new CloseShieldInputStream(Channels.newInputStream(channel))); // keep JAXB from closing the stream
						} catch (Exception e) {
							debug.log(Level.SEVERE, "Failed to load rename history", e);
						}
					}

					// write new combined history
					history.addAll(sessionHistory.sequences());

					channel.position(0);
					History.exportHistory(history, new CloseShieldOutputStream(Channels.newOutputStream(channel))); // keep JAXB from closing the stream

					sessionHistory.clear();
					persistentHistoryTotalSize = history.totalSize();
				}
			}
		} catch (Exception e) {
			debug.log(Level.SEVERE, "Failed to write rename history", e);
		}
	}

	public synchronized void append(Map<File, File> elements) {
		append(elements.entrySet());
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
