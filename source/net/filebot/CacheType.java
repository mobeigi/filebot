package net.filebot;

import java.time.Duration;

import net.sf.ehcache.config.CacheConfiguration;

public enum CacheType {

	Persistent(Duration.ofDays(180), true),

	Monthly(Duration.ofDays(60), true),

	Weekly(Duration.ofDays(12), true),

	Daily(Duration.ofDays(1), true),

	Ephemeral(Duration.ofHours(2), false);

	final long timeToLiveSeconds;
	final boolean diskPersistent;

	CacheType(Duration timeToLive, boolean diskPersistent) {
		this.timeToLiveSeconds = timeToLive.getSeconds();
		this.diskPersistent = diskPersistent;
	}

	CacheConfiguration getConfiguration(String name) {
		// Strategy.LOCALTEMPSWAP is not restartable so we can't but use the deprecated disk persistent code (see http://stackoverflow.com/a/24623527/1514467)
		return new CacheConfiguration().name(name).maxEntriesLocalHeap(diskPersistent ? 200 : 0).maxEntriesLocalDisk(0).eternal(false).timeToLiveSeconds(timeToLiveSeconds).timeToIdleSeconds(timeToLiveSeconds).overflowToDisk(diskPersistent).diskPersistent(diskPersistent);
	}

}
