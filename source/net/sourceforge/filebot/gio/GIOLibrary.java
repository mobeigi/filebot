
package net.sourceforge.filebot.gio;


import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;


interface GIOLibrary extends Library {
	
	GIOLibrary INSTANCE = (GIOLibrary) Native.loadLibrary("gio-2.0", GIOLibrary.class);
	
	
	void g_type_init();
	
	
	Pointer g_vfs_get_default();
	
	
	Pointer g_vfs_get_file_for_uri(Pointer gvfs, String uri);
	
	
	Pointer g_file_get_path(Pointer gfile);
	
	
	void g_free(Pointer gpointer);
	
	
	void g_object_unref(Pointer gobject);
	
}
