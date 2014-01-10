package net.sourceforge.tuned.ui;

import static java.util.Collections.*;
import static javax.swing.JOptionPane.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Window;
import java.awt.dnd.DnDConstants;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.basic.BasicTableUI;
import javax.swing.text.JTextComponent;
import javax.swing.undo.UndoManager;

public final class TunedUtilities {

	public static final Color TRANSLUCENT = new Color(255, 255, 255, 0);

	public static void checkEventDispatchThread() {
		if (!SwingUtilities.isEventDispatchThread()) {
			throw new IllegalStateException("Method must be accessed from the Swing Event Dispatch Thread, but was called on Thread \"" + Thread.currentThread().getName() + "\"");
		}
	}

	public static Color interpolateHSB(Color c1, Color c2, float f) {
		float[] hsb1 = Color.RGBtoHSB(c1.getRed(), c1.getGreen(), c1.getBlue(), null);
		float[] hsb2 = Color.RGBtoHSB(c2.getRed(), c2.getGreen(), c2.getBlue(), null);
		float[] hsb = new float[3];

		for (int i = 0; i < hsb.length; i++) {
			hsb[i] = hsb1[i] + ((hsb2[i] - hsb1[i]) * f);
		}

		return Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
	}

	public static String escapeHTML(String s) {
		char[] sc = new char[] { '&', '<', '>', '"', '\'' };
		for (char c : sc) {
			s = s.replace(Character.toString(c), String.format("&#%d;", (int) c)); // e.g. &#38;
		}
		return s;
	}

	public static Color derive(Color color, float alpha) {
		return new Color(((int) ((alpha * 255)) << 24) | (color.getRGB() & 0x00FFFFFF), true);
	}

	public static String toHex(Color c) {
		return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
	}

	public static boolean isShiftOrAltDown(ActionEvent evt) {
		return checkModifiers(evt.getModifiers(), ActionEvent.SHIFT_MASK) || checkModifiers(evt.getModifiers(), ActionEvent.ALT_MASK);
	}

	public static boolean checkModifiers(int modifiers, int mask) {
		return ((modifiers & mask) == mask);
	}

	public static JButton createImageButton(Action action) {
		JButton button = new JButton(action);
		button.setHideActionText(true);
		button.setOpaque(false);

		return button;
	}

	public static void installAction(JComponent component, KeyStroke keystroke, Action action) {
		Object key = action.getValue(Action.NAME);

		if (key == null)
			throw new IllegalArgumentException("Action must have a name");

		component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(keystroke, key);
		component.getActionMap().put(key, action);
	}

	public static UndoManager installUndoSupport(JTextComponent component) {
		final UndoManager undoSupport = new UndoManager();

		// install undo listener
		component.getDocument().addUndoableEditListener(undoSupport);

		// install undo action
		installAction(component, KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_MASK), new AbstractAction("Undo") {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (undoSupport.canUndo())
					undoSupport.undo();
			}
		});

		// install redo action
		installAction(component, KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_MASK), new AbstractAction("Redo") {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (undoSupport.canRedo())
					undoSupport.redo();
			}
		});

		return undoSupport;
	}

	public static boolean isMaximized(Frame frame) {
		return (frame.getExtendedState() & Frame.MAXIMIZED_BOTH) != 0;
	}

	public static List<String> showMultiValueInputDialog(final String text, final String initialValue, final String title, final Component parent) throws InvocationTargetException, InterruptedException {
		String input = showInputDialog(text, initialValue, title, parent);
		if (input == null || input.isEmpty()) {
			return emptyList();
		}

		for (char separator : new char[] { '|', ';' }) {
			if (input.indexOf(separator) >= 0) {
				List<String> values = new ArrayList<String>();
				for (String field : input.split(Pattern.quote(Character.toString(separator)))) {
					field = field.trim();

					if (field.length() > 0) {
						values.add(field);
					}
				}

				if (values.size() > 0) {
					return values;
				}
			}
		}

		return singletonList(input);
	}

	public static String showInputDialog(final String text, final String initialValue, final String title, final Component parent) throws InvocationTargetException, InterruptedException {
		final StringBuilder buffer = new StringBuilder();

		Runnable runnable = new Runnable() {

			@Override
			public void run() {
				Object value = JOptionPane.showInputDialog(parent, text, title, PLAIN_MESSAGE, null, null, initialValue);
				if (value != null) {
					buffer.append(value.toString().trim());
				}
			}
		};
		if (SwingUtilities.isEventDispatchThread()) {
			runnable.run();
		} else {
			SwingUtilities.invokeAndWait(runnable);
		}

		return buffer.length() == 0 ? null : buffer.toString();
	}

	public static Window getWindow(Object component) {
		if (component instanceof Window)
			return (Window) component;

		if (component instanceof Component)
			return SwingUtilities.getWindowAncestor((Component) component);

		return null;
	}

	public static Point getOffsetLocation(Window owner) {
		if (owner == null) {
			Window[] toplevel = Window.getOwnerlessWindows();

			if (toplevel.length == 0)
				return new Point(120, 80);

			// assume first top-level window as point of reference
			owner = toplevel[0];
		}

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

	/**
	 * When trying to drag a row of a multi-select JTable, it will start selecting rows instead of initiating a drag. This TableUI will give the JTable proper dnd behaviour.
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
