
package net.sourceforge.tuned.ui;


import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;


public class TunedUtil {
	
	public static void registerActionForKeystroke(JComponent component, KeyStroke keystroke, Action action) {
		Integer key = action.hashCode();
		component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(keystroke, key);
		component.getActionMap().put(key, action);
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
