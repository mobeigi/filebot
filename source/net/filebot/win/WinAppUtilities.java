package net.filebot.win;

import static net.filebot.Logging.*;

import java.util.logging.Level;

import com.sun.jna.Pointer;
import com.sun.jna.WString;
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
			PointerByReference r = new PointerByReference();
			if (Shell32.INSTANCE.GetCurrentProcessExplicitAppUserModelID(r).longValue() != 0) {
				Pointer p = r.getPointer();
				return p.getWideString(0); // LEAK NATIVE MEMORY
			}
		} catch (Throwable t) {
			debug.log(Level.WARNING, t.getMessage(), t);
		}
		return null;
	}

}
