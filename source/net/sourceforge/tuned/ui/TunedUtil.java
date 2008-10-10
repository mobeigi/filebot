
package net.sourceforge.tuned.ui;


import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;


public final class TunedUtil {
	
	public static void checkEventDispatchThread() {
		if (!SwingUtilities.isEventDispatchThread()) {
			throw new IllegalStateException("Method must be accessed from the Swing Event Dispatch Thread, but was called on Thread \"" + Thread.currentThread().getName() + "\"");
		}
	}
	

	public static void putActionForKeystroke(JComponent component, KeyStroke keystroke, Action action) {
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
	

	public static Image getImage(Icon icon) {
		if (icon instanceof ImageIcon) {
			return ((ImageIcon) icon).getImage();
		}
		
		BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
		
		Graphics2D g2d = image.createGraphics();
		icon.paintIcon(null, g2d, 0, 0);
		g2d.dispose();
		
		return image;
	}
	

	public static Timer invokeLater(int delay, final Runnable runnable) {
		Timer timer = new Timer(delay, new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				runnable.run();
			}
			
		});
		
		timer.setRepeats(false);
		timer.start();
		
		return timer;
	}
	

	/**
	 * Dummy constructor to prevent instantiation.
	 */
	private TunedUtil() {
		throw new UnsupportedOperationException();
	}
	
}
