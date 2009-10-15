
package net.sourceforge.tuned;


import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;


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
			addShutdownHook();
			
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
			// remove existing shutdown hook, if any
			removeShutdownHook();
		}
		
		scheduledFuture = executor.schedule(runnable, delay, unit);
	}
	

	public synchronized void cancel() {
		removeShutdownHook();
		
		// stop executor
		executor.shutdownNow();
		
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
