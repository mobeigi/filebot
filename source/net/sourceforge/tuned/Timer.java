package net.sourceforge.tuned;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class Timer implements Runnable {

	private final ThreadFactory threadFactory = new DefaultThreadFactory("Timer", Thread.NORM_PRIORITY, true);

	private ScheduledThreadPoolExecutor executor;
	private ScheduledFuture<?> scheduledFuture;
	private Thread shutdownHook;

	public synchronized void set(long delay, TimeUnit unit, boolean runBeforeShutdown) {
		// create executor if necessary
		if (executor == null) {
			executor = new ScheduledThreadPoolExecutor(1, threadFactory);
		}

		// cancel existing future task
		if (scheduledFuture != null) {
			scheduledFuture.cancel(true);
		}

		Runnable runnable = this;

		if (runBeforeShutdown) {
			try {
				addShutdownHook();
			} catch (Exception e) {
				// may fail if running with restricted permissions
				Logger.getLogger(getClass().getName()).log(Level.WARNING, e.getClass().getName() + ": " + e.getMessage());
			}

			// remove shutdown hook after execution
			runnable = new Runnable() {

				@Override
				public void run() {
					try {
						Timer.this.run();
					} finally {
						cancel();
					}
				}
			};
		} else {
			try {
				// remove existing shutdown hook, if any
				removeShutdownHook();
			} catch (Exception e) {
				// may fail if running with restricted permissions
				Logger.getLogger(getClass().getName()).log(Level.WARNING, e.getClass().getName() + ": " + e.getMessage());
			}
		}

		scheduledFuture = executor.schedule(runnable, delay, unit);
	}

	public synchronized void cancel() {
		removeShutdownHook();
		if (executor != null) {
			executor.shutdownNow();
		}

		scheduledFuture = null;
		executor = null;
	}

	private synchronized void addShutdownHook() {
		if (shutdownHook == null) {
			shutdownHook = new Thread(this);
			Runtime.getRuntime().addShutdownHook(shutdownHook);
		}
	}

	private synchronized void removeShutdownHook() {
		if (shutdownHook != null) {
			try {
				if (shutdownHook != Thread.currentThread()) {
					// can't remove shutdown hooks anymore, once runtime is shutting down,
					// so don't remove the shutdown hook, if we are running on the shutdown hook
					Runtime.getRuntime().removeShutdownHook(shutdownHook);
				}
			} finally {
				shutdownHook = null;
			}
		}
	}

}
