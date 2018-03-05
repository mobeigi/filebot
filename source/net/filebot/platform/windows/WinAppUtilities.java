package net.filebot.platform.windows;

import static javax.swing.BorderFactory.*;
import static net.filebot.Logging.*;

import java.awt.Color;
import java.util.logging.Level;

import javax.swing.UIManager;

import com.sun.jna.WString;
import com.sun.jna.platform.win32.Shell32;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.ptr.PointerByReference;

import net.miginfocom.layout.PlatformDefaults;

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

	public static void initializeApplication() {
		// improved UI defaults
		UIManager.put("TitledBorder.border", createCompoundBorder(createLineBorder(new Color(0xD7D7D7), 1, true), createCompoundBorder(createMatteBorder(6, 5, 6, 5, new Color(0xE5E5E5)), createEmptyBorder(0, 2, 0, 2))));

		// disable MigLayout scaling to fix layout on high-resolution screens (see https://github.com/mikaelgrev/miglayout/issues/53)
		PlatformDefaults.setLogicalPixelBase(PlatformDefaults.BASE_REAL_PIXEL);
	}

	private WinAppUtilities() {
		throw new UnsupportedOperationException();
	}

}
