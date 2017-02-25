
package net.filebot.gio;

import java.io.File;
import java.net.URI;
import java.util.Optional;

public class PlatformGVFS implements GVFS {

	private final File gvfs;

	public PlatformGVFS(File gvfs) {
		if (gvfs.list() == null) {
			throw new IllegalArgumentException(gvfs.getPath() + " is not a valid directory");
		}

		this.gvfs = gvfs;
	}

	public File getPathForURI(URI uri) {
		if ("file".equals(uri.getScheme())) {
			return new File(uri);
		}

		// e.g. smb://10.0.1.5/data/Movies/Avatar.mp4 -> /run/user/1000/gvfs/smb-share:server=10.0.1.5,share=data/Movies/Avatar.mp4
		// e.g. afp://reinhard@10.0.1.5/data/Movies/Avatar.mp4 -> /run/user/1000/gvfs/afp-volume:host=10.0.1.5,user=reinhard,volume=data/Movies/Avatar.mp4
		// e.g. sftp://reinhard@10.0.1.5/home/Movies/Avatar.mp4 -> /run/user/1000/gvfs/sftp:host=10.0.1.5,user=reinhard/home/Movies/Avatar.mp4

		String protocol = uri.getScheme();
		String host = uri.getHost();
		String user = uri.getUserInfo();
		String port = Optional.of(uri.getPort()).filter(i -> i > 0).map(Object::toString).orElse(null);

		String path = uri.getPath().substring(1);
		String volume = null;

		if (protocol.equals("smb") || protocol.equals("afp")) {
			volume = path.substring(0, path.indexOf('/'));
			path = path.substring(volume.length()).substring(1);
		}

		// guess GVFS folder based on keywords (see https://wiki.gnome.org/Projects/gvfs/doc)
		for (String mount : gvfs.list()) {
			if (!mount.startsWith(protocol))
				continue;
			if (!mount.contains(host) && !(mount.endsWith("server=" + host) || mount.endsWith("host=" + host)))
				continue;
			if (volume != null && !(mount.endsWith("share=" + volume) || mount.endsWith("volume=" + volume)))
				continue;
			if (user != null && !mount.contains("user=" + user))
				continue;
			if (port != null && !mount.contains("port=" + port))
				continue;

			return new File(new File(gvfs, mount), path);
		}

		throw new IllegalArgumentException("Failed to locate local path: " + uri);
	}

}
