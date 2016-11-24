
package net.filebot.gio;

import java.io.File;
import java.net.URI;

public class PlatformGVFS implements GVFS {

	private final File gvfs;

	public PlatformGVFS(File gvfs) {
		this.gvfs = gvfs;
	}

	public File getPathForURI(URI uri) {
		if ("file".equals(uri.getScheme())) {
			return new File(uri);
		}

		// e.g. smb://10.0.1.5/data/Movies/Avatar.mp4 -> /run/user/1000/gvfs/smb-share:server=10.0.1.5,share=data/Movies/Avatar.mp4
		// e.g. afp://reinhard@10.0.1.5/data/Movies/Avatar.mp4 -> /run/user/1000/gvfs/afp-volume:host=10.0.1.5,user=reinhard,volume=data/Movies/Avatar.mp4
		// e.g. sftp://reinhard@10.0.1.5/home/Movies/Avatar.mp4 -> /run/user/1000/gvfs/sftp:host=10.0.1.5,user=reinhard/home/Movies/Avatar.mp4

		// guess GVFS folder based on keywords (see https://wiki.gnome.org/Projects/gvfs/doc)
		for (String mount : gvfs.list()) {
			if (mount.startsWith(uri.getScheme()) && mount.contains(uri.getHost())) {
				if (uri.getUserInfo() != null && !mount.contains(uri.getUserInfo()))
					continue;
				if (uri.getPort() > 0 && !mount.contains(String.valueOf(uri.getPort())))
					continue;

				String path = uri.getPath().substring(1);
				String share = path.substring(0, path.indexOf('/'));

				if (mount.endsWith(share)) {
					path = path.substring(share.length()).substring(1);
				}

				return new File(new File(gvfs, mount), path);
			}
		}

		throw new IllegalArgumentException("Failed to locate local path: " + uri);
	}

}
