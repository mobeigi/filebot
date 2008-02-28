
package net.sourceforge.filebot;


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
	

	public static Throwable getRootCause(Throwable t) {
		while (t.getCause() != null) {
			t = t.getCause();
		}
		
		return t;
	}
	
}
