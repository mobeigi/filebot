
package net.sourceforge.filebot.ui;


import java.awt.datatransfer.DataFlavor;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;


public class FileBotUtil {
	
	private FileBotUtil() {
		
	}
	

	public static void registerActionForKeystroke(JComponent component, KeyStroke keystroke, Action action) {
		Integer key = action.hashCode();
		component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(keystroke, key);
		component.getActionMap().put(key, action);
	}
	

	/**
	 * Strip string of invalid characters
	 * 
	 * @param filename original filename
	 * @return filename stripped of invalid characters
	 */
	public static String validateFileName(String filename) {
		// strip \, /, :, *, ?, ", <, > and |
		return filename.replaceAll("[\\\\/:*?\"<>|]", "");
	}
	
	public static final DataFlavor uriListFlavor = createUriListFlavor();
	
	
	private static DataFlavor createUriListFlavor() {
		try {
			return new DataFlavor("text/uri-list;class=java.lang.String");
		} catch (ClassNotFoundException e) {
			// will never happen
			Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, e.getMessage(), e);
		}
		
		return null;
	}
	

	public static Throwable getRootCause(Throwable t) {
		while (t.getCause() != null) {
			t = t.getCause();
		}
		
		return t;
	}
	
}
