
package net.sourceforge.filebot.gio;


import java.io.File;
import java.net.URI;

import com.sun.jna.Platform;
import com.sun.jna.Pointer;


public class GVFS {
	
	private static Pointer gvfs;
	
	
	private synchronized static Pointer getDefaultVFS() {
		if (gvfs == null) {
			GIOLibrary.INSTANCE.g_type_init();
			gvfs = GIOLibrary.INSTANCE.g_vfs_get_default();
		}
		return gvfs;
	}
	
	
	public static File getPathForURI(URI uri) {
		Pointer gfile = GIOLibrary.INSTANCE.g_vfs_get_file_for_uri(getDefaultVFS(), uri.toString());
		Pointer chars = GIOLibrary.INSTANCE.g_file_get_path(gfile);
		
		try {
			if (chars != null)
				return new File(chars.getString(0));
			else
				return null;
		} finally {
			GIOLibrary.INSTANCE.g_object_unref(gfile);
			GIOLibrary.INSTANCE.g_free(chars);
		}
	}
	
	
	public static boolean isSupported() {
		return Platform.isLinux() || Platform.isFreeBSD();
	}
	
}
