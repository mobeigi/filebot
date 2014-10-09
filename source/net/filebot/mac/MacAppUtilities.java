package net.filebot.mac;

import java.awt.EventQueue;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.UIManager;

import ca.weblite.objc.Client;

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;

public class MacAppUtilities {

	private static Client _objc;

	public static Client objc() {
		if (_objc == null) {
			_objc = new Client();
		}
		return _objc;
	}

	public static Object NSData_initWithBase64Encoding(String text) {
		return objc().sendProxy("NSData", "data").send("initWithBase64Encoding:", text);
	}

	public static String NSURL_bookmarkDataWithOptions(String path) {
		return objc().sendProxy("NSURL", "fileURLWithPath:", path).sendProxy("bookmarkDataWithOptions:includingResourceValuesForKeys:relativeToURL:error:", 2048, null, null, null).sendString("base64Encoding");
	}

	public static Object NSURL_URLByResolvingBookmarkData_startAccessingSecurityScopedResource(String text) {
		return objc().sendProxy("NSURL", "URLByResolvingBookmarkData:options:relativeToURL:bookmarkDataIsStale:error:", NSData_initWithBase64Encoding(text), 1024, null, false, null).send("startAccessingSecurityScopedResource");
	}

	public static void setWindowCanFullScreen(Window window) {
		try {
			com.apple.eawt.FullScreenUtilities.setWindowCanFullScreen(window, true);
		} catch (Throwable t) {
			Logger.getLogger(MacAppUtilities.class.getName()).log(Level.WARNING, "setWindowCanFullScreen not supported: " + t);
		}
	}

	public static void requestForeground() {
		try {
			com.apple.eawt.Application.getApplication().requestForeground(true);
		} catch (Throwable t) {
			Logger.getLogger(MacAppUtilities.class.getName()).log(Level.WARNING, "requestForeground not supported: " + t);
		}
	}

	public static void revealInFinder(File file) {
		try {
			com.apple.eio.FileManager.revealInFinder(file);
		} catch (Throwable t) {
			Logger.getLogger(MacAppUtilities.class.getName()).log(Level.WARNING, "revealInFinder not supported: " + t);
		}
	}

	public static void initializeApplication() {
		// improved UI defaults
		UIManager.put("TitledBorder.border", UIManager.getBorder("InsetBorder.aquaVariant"));

		// make sure Application Quit Events get forwarded to normal Window Listeners
		Application.getApplication().addApplicationListener(new ApplicationAdapter() {

			@Override
			public void handleQuit(ApplicationEvent evt) {
				for (Window window : Window.getOwnerlessWindows()) {
					// close all windows
					window.setVisible(false);

					// call window listeners
					EventQueue.invokeLater(() -> {
						window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
					});
				}
			}
		});
	}

	public static boolean isLockedFolder(File folder) {
		return folder.isDirectory() && !folder.canRead() && !folder.canWrite();
	}

}
