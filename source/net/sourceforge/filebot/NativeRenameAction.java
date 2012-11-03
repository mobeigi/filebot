
package net.sourceforge.filebot;


import static java.util.Collections.*;
import static net.sourceforge.tuned.FileUtilities.*;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CancellationException;

import com.sun.jna.Platform;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.Shell32;
import com.sun.jna.platform.win32.ShellAPI;
import com.sun.jna.platform.win32.ShellAPI.SHFILEOPSTRUCT;


public enum NativeRenameAction implements RenameAction {
	
	MOVE,
	
	COPY;
	
	@Override
	public File rename(File src, File dst) throws IOException {
		dst = resolveDestination(src, dst, false).getCanonicalFile();
		rename(singletonMap(src, dst));
		return dst;
	}
	
	
	public void rename(Map<File, File> map) throws IOException {
		String[] src = new String[map.size()];
		String[] dst = new String[map.size()];
		
		// resolve paths
		int i = 0;
		for (Entry<File, File> it : map.entrySet()) {
			src[i] = it.getKey().getCanonicalPath();
			dst[i] = resolveDestination(it.getKey(), it.getValue(), false).getCanonicalPath();
			i++;
		}
		
		callNative_Shell32(this, src, dst);
	}
	
	
	private static void callNative_Shell32(NativeRenameAction action, String[] src, String[] dst) {
		// configure parameter structure
		SHFILEOPSTRUCT op = new SHFILEOPSTRUCT();
		op.wFunc = (action == MOVE) ? ShellAPI.FO_MOVE : ShellAPI.FO_COPY;
		op.fFlags = Shell32.FOF_MULTIDESTFILES | Shell32.FOF_NOCONFIRMATION | Shell32.FOF_NOCONFIRMMKDIR;
		
		op.pFrom = new WString(op.encodePaths(src));
		op.pTo = new WString(op.encodePaths(dst));
		
		Shell32.INSTANCE.SHFileOperation(op);
		
		if (op.fAnyOperationsAborted) {
			throw new CancellationException();
		}
	}
	
	
	public static boolean isSupported() {
		try {
			return Platform.isWindows();
		} catch (Throwable e) {
			return false;
		}
	}
	
}
