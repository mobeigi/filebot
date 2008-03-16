
package net.sourceforge.filebot;


import java.util.Iterator;

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
	 * invalid characters: \, /, :, *, ?, ", <, > and |
	 */
	public static final String INVALID_CHARACTERS = "\\/:*?\"<>|";
	
	
	/**
	 * Strip string of invalid characters
	 * 
	 * @param filename original filename
	 * @return filename stripped of invalid characters
	 */
	public static String validateFileName(String filename) {
		// strip  \, /, :, *, ?, ", <, > and |
		return filename.replaceAll(String.format("[%s]+", INVALID_CHARACTERS), "");
	}
	

	public static Throwable getRootCause(Throwable t) {
		while (t.getCause() != null) {
			t = t.getCause();
		}
		
		return t;
	}
	

	public static String join(Iterable<?> list, String delim) {
		StringBuilder sb = new StringBuilder();
		
		Iterator<?> it = list.iterator();
		
		while (it.hasNext()) {
			sb.append(it.next().toString());
			
			if (it.hasNext())
				sb.append(delim);
		}
		
		return sb.toString();
	}
}
