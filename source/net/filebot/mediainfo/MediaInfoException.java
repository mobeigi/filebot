package net.filebot.mediainfo;

import com.sun.jna.Platform;

public class MediaInfoException extends RuntimeException {

	public MediaInfoException(String message) {
		super(message);
	}

	public MediaInfoException(LinkageError e) {
		super(getLinkageErrorMessage(e), e);
	}

	private static String getLinkageErrorMessage(LinkageError e) {
		String name = Platform.isWindows() ? "MediaInfo.dll" : Platform.isMac() ? "libmediainfo.dylib" : "libmediainfo.so";
		String arch = System.getProperty("os.arch");
		return String.format("Unable to load %s native library %s: %s", arch, name, e.getMessage());
	}

}
