
package net.sourceforge.filebot.ui;


import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;


public class FileBotUtil {
	
	private FileBotUtil() {
		// hide construktor
	}
	

	public static void registerActionForKeystroke(JComponent component, KeyStroke keystroke, Action action) {
		Integer key = action.hashCode();
		component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(keystroke, key);
		component.getActionMap().put(key, action);
	}
	
}
