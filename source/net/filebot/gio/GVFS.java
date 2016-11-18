package net.filebot.gio;

import java.io.File;
import java.net.URI;

import net.filebot.util.SystemProperty;

public interface GVFS {

	File getPathForURI(URI uri);

	public static GVFS getDefaultVFS() {
		return SystemProperty.of("net.filebot.gio.GVFS", path -> new PlatformGVFS(new File(path)), NativeGVFS::new).get();
	}

}
