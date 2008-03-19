
package net.sourceforge.filebot;


import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.util.Iterator;
import java.util.regex.Pattern;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JDialog;
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
	public static final Pattern INVALID_CHARACTERS_PATTERN = Pattern.compile(String.format("[%s]+", Pattern.quote(INVALID_CHARACTERS)));
	
	
	/**
	 * Strip string of invalid characters
	 * 
	 * @param filename original filename
	 * @return filename stripped of invalid characters
	 */
	public static String validateFileName(String filename) {
		// strip  \, /, :, *, ?, ", <, > and |
		return INVALID_CHARACTERS_PATTERN.matcher(filename).replaceAll("");
	}
	

	public static boolean isInvalidFileName(String filename) {
		return INVALID_CHARACTERS_PATTERN.matcher(filename).find();
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
	

	public static Point getPreferredLocation(JDialog dialog) {
		Window owner = dialog.getOwner();
		
		if (owner == null)
			return new Point(120, 80);
		
		Point p = owner.getLocation();
		Dimension d = owner.getSize();
		
		return new Point(p.x + d.width / 4, p.y + d.height / 7);
	}
}
