package net.filebot.mediainfo;

import com.sun.jna.Platform;

public class MediaInfoException extends RuntimeException {

	public MediaInfoException() {
		super(String.format("Unable to load %d-bit native library 'mediainfo'", Platform.is64Bit() ? 64 : 32));
	}

	public MediaInfoException(LinkageError e) {
		super(String.format("Unable to load %d-bit native library 'mediainfo'", Platform.is64Bit() ? 64 : 32), e);
	}

	public MediaInfoException(String msg, Throwable e) {
		super(msg, e);
	}

}
