
package net.sourceforge.filebot.cli;


import static java.nio.file.StandardWatchEventKinds.*;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.tuned.DefaultThreadFactory;
import net.sourceforge.tuned.Timer;


public abstract class FolderWatchService implements Closeable {
	
	private final Collection<File> commitSet = new HashSet<File>();
	
	private final ExecutorService processor = Executors.newSingleThreadExecutor();
	private final ExecutorService watchers = Executors.newCachedThreadPool(new DefaultThreadFactory("FolderWatchService", Thread.MIN_PRIORITY, true));
	
	private long commitDelay = 500; // 0.5 s
	private final Timer commitTimer = new Timer() {
		
		@Override
		public void run() {
			synchronized (processor) {
				commit();
			}
		}
	};
	
	
	public synchronized void setCommitDelay(long commitDelay) {
		if (commitDelay < 0)
			throw new IllegalArgumentException("Delay must not be negativ");
		
		this.commitDelay = commitDelay;
		resetCommitTimer();
	}
	
	
	public synchronized void resetCommitTimer() {
		commitTimer.set(commitDelay, TimeUnit.MILLISECONDS, false);
	}
	
	
	public synchronized void commit() {
		final SortedSet<File> files = new TreeSet<File>();
		
		synchronized (commitSet) {
			files.addAll(commitSet);
			commitSet.clear();
		}
		
		if (files.isEmpty()) {
			return;
		}
		
		processor.submit(new Runnable() {
			
			@Override
			public void run() {
				synchronized (processor) {
					processCommitSet(files.toArray(new File[0]));
				}
			}
		});
	}
	
	
	public abstract void processCommitSet(File[] files);
	
	
	public synchronized void watch(File node) throws IOException {
		if (!node.isDirectory()) {
			throw new IllegalArgumentException("Must be a folder: " + node);
		}
		
		watchers.submit(new FolderWatcher(node) {
			
			@Override
			protected void processEvents(List<WatchEvent<?>> events) {
				synchronized (commitSet) {
					resetCommitTimer();
					super.processEvents(events);
				}
			}
			
			
			@Override
			protected void created(File file) {
				commitSet.add(file);
			}
			
			
			@Override
			protected void modified(File file) {
				commitSet.add(file);
			}
			
			
			@Override
			protected void deleted(File file) {
				commitSet.remove(file);
			}
		});
	}
	
	
	@Override
	public synchronized void close() throws IOException {
		commitTimer.cancel();
		processor.shutdownNow();
		watchers.shutdownNow();
	}
	
	
	private abstract static class FolderWatcher implements Runnable, Closeable {
		
		private final Path node;
		private final WatchService watchService;
		
		
		public FolderWatcher(File node) throws IOException {
			this.node = node.toPath();
			this.watchService = this.node.getFileSystem().newWatchService();
			this.node.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
		}
		
		
		@Override
		public void run() {
			try {
				watch();
			} catch (InterruptedException e) {
				// ignore, part of an orderly shutdown
			} catch (Exception e) {
				Logger.getLogger(getClass().getName()).log(Level.WARNING, e.getMessage(), e);
			}
		}
		
		
		public void watch() throws IOException, InterruptedException {
			try {
				while (true) {
					WatchKey key = watchService.take();
					processEvents(key.pollEvents());
					key.reset();
				}
			} finally {
				this.close();
			}
		}
		
		
		public File getAbsoluteFile(WatchEvent event) {
			return node.resolve(event.context().toString()).toFile();
		}
		
		
		protected void processEvents(List<WatchEvent<?>> list) {
			for (WatchEvent event : list) {
				if (event.kind() == ENTRY_CREATE) {
					created(getAbsoluteFile(event));
				} else if (event.kind() == ENTRY_MODIFY) {
					modified(getAbsoluteFile(event));
				} else if (event.kind() == ENTRY_DELETE) {
					deleted(getAbsoluteFile(event));
				}
			}
		}
		
		
		protected abstract void created(File file);
		
		
		protected abstract void modified(File file);
		
		
		protected abstract void deleted(File file);
		
		
		@Override
		public void close() throws IOException {
			watchService.close();
		}
	}
	
}
