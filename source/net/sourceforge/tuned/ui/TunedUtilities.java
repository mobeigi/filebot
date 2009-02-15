
package net.sourceforge.tuned.ui;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Window;
import java.awt.dnd.DnDConstants;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Method;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.basic.BasicTableUI;
import net.sourceforge.tuned.ExceptionUtilities;


public final class TunedUtilities {
	
	public static final Color TRANSLUCENT = new Color(255, 255, 255, 0);
	
	
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
		if (icon == null)
			return null;
		
		if (icon instanceof ImageIcon)
			return ((ImageIcon) icon).getImage();
		
		// draw icon into a new image
		BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
		
		Graphics2D g2d = image.createGraphics();
		icon.paintIcon(null, g2d, 0, 0);
		g2d.dispose();
		
		return image;
	}
	

	public static Dimension getDimension(Icon icon) {
		return new Dimension(icon.getIconWidth(), icon.getIconHeight());
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
	

	public static void syncPropertyChangeEvents(Class<?> propertyType, String property, Object from, Object to) {
		PropertyChangeDelegate.create(propertyType, property, from, to);
	}
	
	
	private static class PropertyChangeDelegate implements PropertyChangeListener {
		
		private final String property;
		
		private final Object target;
		private final Method firePropertyChange;
		
		
		public static PropertyChangeDelegate create(Class<?> propertyType, String property, Object source, Object target) {
			try {
				
				PropertyChangeDelegate listener = new PropertyChangeDelegate(propertyType, property, target);
				source.getClass().getMethod("addPropertyChangeListener", PropertyChangeListener.class).invoke(source, listener);
				
				return listener;
			} catch (Exception e) {
				throw ExceptionUtilities.asRuntimeException(e);
			}
		}
		

		protected PropertyChangeDelegate(Class<?> propertyType, String property, Object target) throws SecurityException, NoSuchMethodException {
			this.property = property;
			this.target = target;
			
			this.firePropertyChange = target.getClass().getMethod("firePropertyChange", String.class, propertyType, propertyType);
		}
		

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			if (property.equals(evt.getPropertyName())) {
				try {
					firePropertyChange.invoke(target, evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
				} catch (Exception e) {
					throw ExceptionUtilities.asRuntimeException(e);
				}
			}
		}
		
	}
	

	/**
	 * When trying to drag a row of a multi-select JTable, it will start selecting rows instead
	 * of initiating a drag. This TableUI will give the JTable proper dnd behaviour.
	 */
	public static class DragDropRowTableUI extends BasicTableUI {
		
		@Override
		protected MouseInputListener createMouseInputListener() {
			return new DragDropRowMouseInputHandler();
		}
		
		
		protected class DragDropRowMouseInputHandler extends MouseInputHandler {
			
			@Override
			public void mouseDragged(MouseEvent e) {
				// Only do special handling if we are drag enabled with multiple selection
				if (table.getDragEnabled() && table.getSelectionModel().getSelectionMode() == ListSelectionModel.MULTIPLE_INTERVAL_SELECTION) {
					table.getTransferHandler().exportAsDrag(table, e, DnDConstants.ACTION_COPY);
				} else {
					super.mouseDragged(e);
				}
			}
		}
	}
	
	
	/**
	 * Dummy constructor to prevent instantiation.
	 */
	private TunedUtilities() {
		throw new UnsupportedOperationException();
	}
	
}
