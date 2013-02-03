
package net.sourceforge.filebot.gio;


import java.io.File;
import java.net.URI;

import com.sun.jna.Native;
import com.sun.jna.Pointer;


public class GVFS {
	
	private static GIOLibrary lib;
	private static Pointer gvfs;
	
	
	private synchronized static GIOLibrary getLibrary() {
		if (lib == null) {
			lib = (GIOLibrary) Native.loadLibrary("gio-2.0", GIOLibrary.class);
		}
		return lib;
	}
	
	
	public synchronized static Pointer getDefaultVFS() {
		if (gvfs == null) {
			gvfs = getLibrary().g_vfs_get_default();
		}
		return gvfs;
	}
	
	
	public static File getPathForURI(URI uri) {
		Pointer gfile = getLibrary().g_vfs_get_file_for_uri(getDefaultVFS(), uri.toString());
		Pointer chars = getLibrary().g_file_get_path(gfile);
		
		try {
			if (chars != null)
				return new File(chars.getString(0));
			else
				return null;
		} finally {
			getLibrary().g_object_unref(gfile);
			getLibrary().g_free(chars);
		}
	}
	
}
