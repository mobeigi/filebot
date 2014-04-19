package net.sourceforge.filebot;

import static net.sourceforge.filebot.Settings.*;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.filebot.History.Element;

import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.commons.io.output.CloseShieldOutputStream;

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
			History history = History.importHistory(new CloseShieldInputStream(Channels.newInputStream(channel))); // keep JAXB from closing the stream
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
				History history = new History();
				if (persistentHistoryFile.length() > 0) {
					try {
						channel.position(0); // rewind
						history = History.importHistory(new CloseShieldInputStream(Channels.newInputStream(channel))); // keep JAXB from closing the stream
					} catch (Exception e) {
						Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Failed to load rename history.", e);
					}
				}
				history.addAll(sessionHistory.sequences());

				channel.position(0);
				History.exportHistory(history, new CloseShieldOutputStream(Channels.newOutputStream(channel))); // keep JAXB from closing the stream

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
