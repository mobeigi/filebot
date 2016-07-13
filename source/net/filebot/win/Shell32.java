package net.filebot.win;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.WString;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

interface Shell32 extends StdCallLibrary {

	Shell32 INSTANCE = (Shell32) Native.loadLibrary("shell32", Shell32.class, W32APIOptions.DEFAULT_OPTIONS);

	NativeLong SetCurrentProcessExplicitAppUserModelID(WString appID);

	NativeLong GetCurrentProcessExplicitAppUserModelID(PointerByReference appID);
}
