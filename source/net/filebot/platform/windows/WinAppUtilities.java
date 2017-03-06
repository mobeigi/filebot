package net.filebot.platform.windows;

import static net.filebot.Logging.*;

import java.util.logging.Level;

import com.sun.jna.WString;
import com.sun.jna.platform.win32.Shell32;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.ptr.PointerByReference;

public class WinAppUtilities {

	public static void setAppUserModelID(String appID) {
		try {
			Shell32.INSTANCE.SetCurrentProcessExplicitAppUserModelID(new WString(appID));
		} catch (Throwable t) {
			debug.log(Level.WARNING, t.getMessage(), t);
		}
	}

	public static String getAppUserModelID() {
		try {
			PointerByReference ppszAppID = new PointerByReference();
			if (Shell32.INSTANCE.GetCurrentProcessExplicitAppUserModelID(ppszAppID) == WinError.S_OK) {
				return ppszAppID.getValue().getWideString(0);
			}
		} catch (Throwable t) {
			debug.log(Level.WARNING, t.getMessage(), t);
		}
		return null;
	}

	private WinAppUtilities() {
		throw new UnsupportedOperationException();
	}

}
