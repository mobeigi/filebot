
package net.sourceforge.filebot;


import static net.sourceforge.tuned.FileUtilities.*;

import java.io.File;
import java.io.IOException;

import com.sun.jna.Platform;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.Shell32;
import com.sun.jna.platform.win32.ShellAPI;
import com.sun.jna.platform.win32.ShellAPI.SHFILEOPSTRUCT;


public enum NativeRenameAction implements RenameAction {
	
	MOVE,
	
	COPY;
	
	@Override
	public File rename(File src, File dst) throws Exception {
		return rename(new File[] { src }, new File[] { dst })[0];
	}
	
	
	public File[] rename(File[] src, File[] dst) throws Exception {
		String[] pFrom = new String[src.length];
		String[] pTo = new String[dst.length];
		File[] result = new File[dst.length];
		
		// resolve paths
		for (int i = 0; i < pFrom.length; i++) {
			result[i] = resolveDestination(src[i], dst[i]).getCanonicalFile(); // resolve dst
			pTo[i] = result[i].getPath();
			pFrom[i] = src[i].getCanonicalPath(); // resolve src
		}
		
		// configure parameter structure
		SHFILEOPSTRUCT op = new SHFILEOPSTRUCT();
		
		op.wFunc = ShellAPI.class.getField("FO_" + name()).getInt(null); // ShellAPI.FO_MOVE | ShellAPI.FO_COPY
		op.fFlags = Shell32.FOF_MULTIDESTFILES | Shell32.FOF_NOCONFIRMMKDIR;
		
		op.pFrom = new WString(op.encodePaths(pFrom));
		op.pTo = new WString(op.encodePaths(pTo));
		
		Shell32.INSTANCE.SHFileOperation(op);
		
		if (op.fAnyOperationsAborted) {
			throw new IOException("Operation Aborted");
		}
		
		return result;
	}
	
	
	public static boolean isSupported(NativeRenameAction action) {
		return Platform.isWindows();
	}
}
