
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
	
	private final Collection<Path> commitSet = new HashSet<Path>();
	
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
			for (Path path : commitSet) {
				files.add(path.toFile());
			}
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
		
		watchers.submit(new FolderWatcher(node.toPath()) {
			
			@Override
			protected void processEvents(List<WatchEvent<?>> events) {
				synchronized (commitSet) {
					resetCommitTimer();
					super.processEvents(events);
				}
			}
			
			
			@Override
			protected void created(Path path) {
				commitSet.add(path);
			}
			
			
			@Override
			protected void modified(Path path) {
				commitSet.add(path);
			}
			
			
			@Override
			protected void deleted(Path path) {
				commitSet.remove(path);
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
		
		
		public FolderWatcher(Path node) throws IOException {
			this.node = node;
			this.watchService = node.getFileSystem().newWatchService();
			node.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
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
		
		
		public Path getPath(WatchEvent event) {
			return node.resolve(event.context().toString());
		}
		
		
		protected void processEvents(List<WatchEvent<?>> list) {
			for (WatchEvent event : list) {
				if (event.kind() == ENTRY_CREATE) {
					created(getPath(event));
				} else if (event.kind() == ENTRY_MODIFY) {
					modified(getPath(event));
				} else if (event.kind() == ENTRY_DELETE) {
					deleted(getPath(event));
				}
			}
		}
		
		
		protected abstract void created(Path path);
		
		
		protected abstract void modified(Path path);
		
		
		protected abstract void deleted(Path path);
		
		
		@Override
		public void close() throws IOException {
			watchService.close();
		}
	}
	
}
