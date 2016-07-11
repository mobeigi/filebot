
package net.filebot.gio;

import java.io.File;
import java.net.URI;

import com.sun.jna.Native;

public class PlatformGVFS implements GVFS {

	private static final LibC lib_c = (LibC) Native.loadLibrary("c", LibC.class);
	private static final String gvfs = "/run/user/" + lib_c.getuid() + "/gvfs/";

	public File getPathForURI(URI uri) {
		// e.g. smb://10.0.1.5/data/Movies/Avatar.mp4 -> /run/user/1000/gvfs/smb-share:server=10.0.1.5,share=data/Movies/Avatar.mp4

		switch (uri.getScheme()) {
		case "file":
			return new File(uri);
		default:
			return new File(gvfs + uri.getScheme() + "-share:server=" + uri.getHost() + ",share=" + uri.getPath().substring(1));
		}
	}

}
