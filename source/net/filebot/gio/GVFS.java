package net.filebot.gio;

import java.io.File;
import java.net.URI;

public interface GVFS {

	File getPathForURI(URI uri);

	public static GVFS getDefaultVFS() {
		return new PlatformGVFS();
	}

}
