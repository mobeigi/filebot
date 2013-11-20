package net.sourceforge.filebot.web;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class FloodLimit {

	private final Semaphore permits;
	private final ScheduledThreadPoolExecutor timer = new ScheduledThreadPoolExecutor(1);

	private final long releaseDelay;
	private final TimeUnit timeUnit;

	public FloodLimit(int permitLimit, long releaseDelay, TimeUnit timeUnit) {
		this.permits = new Semaphore(permitLimit, true);
		this.releaseDelay = releaseDelay;
		this.timeUnit = timeUnit;
	}

	public ScheduledFuture<?> acquirePermit() throws InterruptedException {
		permits.acquire();
		return timer.schedule(new ReleasePermit(), releaseDelay, timeUnit);
	}

	public void releaseNow(ScheduledFuture<?> future) {
		if (future.cancel(false)) {
			permits.release();
		}
	}

	private class ReleasePermit implements Runnable {

		@Override
		public void run() {
			permits.release();
		}
	}
}
