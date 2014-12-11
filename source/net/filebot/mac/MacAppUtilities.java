package net.filebot.mac;

import java.awt.Window;
import java.io.File;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.UIManager;

import ca.weblite.objc.Client;

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
			Class<?> fullScreenUtilities = Class.forName("com.apple.eawt.FullScreenUtilities");
			Method setWindowCanFullScreen = fullScreenUtilities.getMethod("setWindowCanFullScreen", new Class<?>[] { Window.class, boolean.class });
			setWindowCanFullScreen.invoke(null, window, true);
		} catch (Throwable t) {
			Logger.getLogger(MacAppUtilities.class.getName()).log(Level.WARNING, "setWindowCanFullScreen not supported: " + t);
		}
	}

	public static void requestForeground() {
		try {
			Class<?> application = Class.forName("com.apple.eawt.Application");
			Object instance = application.getMethod("getApplication").invoke(null);
			Method requestForeground = application.getMethod("requestForeground", new Class<?>[] { boolean.class });
			requestForeground.invoke(instance, true);
		} catch (Throwable t) {
			Logger.getLogger(MacAppUtilities.class.getName()).log(Level.WARNING, "requestForeground not supported: " + t);
		}
	}

	public static void revealInFinder(File file) {
		try {
			Class<?> fileManager = Class.forName("com.apple.eio.FileManager");
			Method revealInFinder = fileManager.getMethod("revealInFinder", new Class<?>[] { File.class });
			revealInFinder.invoke(null, file);
		} catch (Throwable t) {
			Logger.getLogger(MacAppUtilities.class.getName()).log(Level.WARNING, "revealInFinder not supported: " + t);
		}
	}

	public static void initializeApplication() {
		// improved UI defaults
		UIManager.put("TitledBorder.border", UIManager.getBorder("InsetBorder.aquaVariant"));

		// make sure Application Quit Events get forwarded to normal Window Listeners
		try {
			Class<?> application = Class.forName("com.apple.eawt.Application");
			Object instance = application.getMethod("getApplication").invoke(null);
			Class<?> quitStrategy = Class.forName("com.apple.eawt.QuitStrategy");
			Method setQuitStrategy = application.getMethod("setQuitStrategy", quitStrategy);
			Object closeAllWindows = quitStrategy.getField("CLOSE_ALL_WINDOWS").get(null);
			setQuitStrategy.invoke(instance, closeAllWindows);
		} catch (Throwable t) {
			Logger.getLogger(MacAppUtilities.class.getName()).log(Level.WARNING, "setQuitStrategy not supported: " + t);
		}
	}

	public static boolean isLockedFolder(File folder) {
		// write permissions my not be available even after sandbox has granted permission (e.g. when accessing files of another user)
		return folder.isDirectory() && !folder.canRead();
	}

	public static boolean askUnlockFolders(final Window owner, final Collection<File> files) {
		return DropToUnlock.showUnlockFoldersDialog(owner, files);
	}

}
