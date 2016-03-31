package net.filebot.util.ui;

import static java.util.Collections.*;
import static javax.swing.JOptionPane.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Window;
import java.awt.dnd.DnDConstants;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
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
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.basic.BasicTableUI;
import javax.swing.text.JTextComponent;
import javax.swing.undo.UndoManager;

import net.filebot.Settings;

public final class SwingUI {

	public static void checkEventDispatchThread() {
		if (!SwingUtilities.isEventDispatchThread()) {
			throw new IllegalStateException("Method must be accessed from the Swing Event Dispatch Thread, but was called on Thread \"" + Thread.currentThread().getName() + "\"");
		}
	}

	public static void runOnEventDispatchThread(Runnable r) {
		if (SwingUtilities.isEventDispatchThread()) {
			r.run();
		} else {
			try {
				SwingUtilities.invokeAndWait(r);
			} catch (InvocationTargetException | InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static final Color TRANSLUCENT = new Color(255, 255, 255, 0);

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
		return c == null ? "inherit" : String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
	}

	public static boolean isShiftOrAltDown(InputEvent evt) {
		return checkModifiers(evt.getModifiers(), ActionEvent.SHIFT_MASK) || checkModifiers(evt.getModifiers(), ActionEvent.ALT_MASK);
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
		button.setToolTipText(String.valueOf(action.getValue(Action.NAME)));
		button.setVerticalTextPosition(SwingConstants.BOTTOM);
		button.setOpaque(false);

		if (Settings.isMacApp()) {
			button.setPreferredSize(new Dimension(28, 27));
		} else {
			button.setPreferredSize(new Dimension(26, 26));
		}

		return button;
	}

	public static void installAction(JComponent component, KeyStroke keystroke, Action action) {
		Object key = action.getValue(Action.NAME);

		if (key == null) {
			throw new IllegalArgumentException("Action must have a name");
		}

		component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(keystroke, key);
		component.getActionMap().put(key, action);

		// automatically add Mac OS X compatibility (on Mac the BACKSPACE key is called DELETE, and there is no DELETE key)
		if (keystroke.getKeyCode() == KeyEvent.VK_DELETE) {
			KeyStroke macKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, keystroke.getModifiers(), keystroke.isOnKeyRelease());
			Object macKey = "mac." + action.getValue(Action.NAME);
			component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(macKeyStroke, macKey);
			component.getActionMap().put(macKey, action);
		}
	}

	public static UndoManager installUndoSupport(JTextComponent component) {
		final UndoManager undoSupport = new UndoManager();

		// install undo listener
		component.getDocument().addUndoableEditListener(undoSupport);

		// install undo action
		installAction(component, KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_MASK), newAction("Undo", evt -> {
			if (undoSupport.canUndo()) {
				undoSupport.undo();
			}
		}));

		// install redo action
		installAction(component, KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_MASK), newAction("Redo", evt -> {
			if (undoSupport.canRedo()) {
				undoSupport.redo();
			}
		}));

		return undoSupport;
	}

	public static boolean isMaximized(Frame frame) {
		return (frame.getExtendedState() & Frame.MAXIMIZED_BOTH) != 0;
	}

	public static List<String> showMultiValueInputDialog(final Object message, final String initialValue, final String title, final Component parent) {
		String input = showInputDialog(message, initialValue, title, parent);
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

	public static String showInputDialog(final Object message, final String initialValue, final String title, final Component parent) {
		final StringBuilder buffer = new StringBuilder();

		runOnEventDispatchThread(() -> {
			Object value = JOptionPane.showInputDialog(parent, message, title, PLAIN_MESSAGE, null, null, initialValue);
			if (value != null) {
				buffer.append(value.toString().trim());
			}
		});

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
		Timer timer = new Timer(delay, (evt) -> {
			runnable.run();
		});

		timer.setRepeats(false);
		timer.start();

		return timer;
	}

	public static void withWaitCursor(Object source, BackgroundRunnable runnable) throws Exception {
		Window window = getWindow(source);
		try {
			window.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			runnable.run();
		} finally {
			window.setCursor(Cursor.getDefaultCursor());
		}
	}

	public static MouseAdapter mouseClicked(Consumer<MouseEvent> handler) {
		return new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent evt) {
				handler.accept(evt);
			}
		};
	}

	public static JButton newButton(String name, Icon icon, Consumer<ActionEvent> action) {
		return new JButton(new LambdaAction(name, icon, action));
	}

	public static Action newAction(String name, Consumer<ActionEvent> action) {
		return new LambdaAction(name, null, action);
	}

	public static Action newAction(String name, Icon icon, Consumer<ActionEvent> action) {
		return new LambdaAction(name, icon, action);
	}

	private static class LambdaAction extends AbstractAction {

		private Consumer<ActionEvent> action;

		public LambdaAction(String name, Icon icon, Consumer<ActionEvent> action) {
			super(name, icon);
			this.action = action;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			action.accept(e);
		}
	}

	public static SwingWorker<Void, Void> newSwingWorker(BackgroundRunnable doInBackground) {
		return new SwingRunnable(doInBackground);
	}

	public static <T> SwingWorker<T, Void> newSwingWorker(BackgroundSupplier<T> doInBackground, Consumer<T> done, Consumer<Exception> error) {
		return new SwingLambda<T, Void>(doInBackground, done, error);
	}

	private static class SwingRunnable extends SwingWorker<Void, Void> {

		private BackgroundRunnable doInBackground;

		public SwingRunnable(BackgroundRunnable doInBackground) {
			this.doInBackground = doInBackground;
		}

		@Override
		protected Void doInBackground() throws Exception {
			doInBackground.run();
			return null;
		}
	}

	@FunctionalInterface
	public static interface BackgroundRunnable {
		void run() throws Exception;
	}

	@FunctionalInterface
	public static interface BackgroundSupplier<T> {
		T get() throws Exception;
	}

	private static class SwingLambda<T, V> extends SwingWorker<T, V> {

		private BackgroundSupplier<T> doInBackground;
		private Consumer<T> done;
		private Consumer<Exception> error;

		public SwingLambda(BackgroundSupplier<T> doInBackground, Consumer<T> done, Consumer<Exception> error) {
			this.doInBackground = doInBackground;
			this.done = done;
			this.error = error;
		}

		@Override
		protected T doInBackground() throws Exception {
			return doInBackground.get();
		}

		@Override
		protected void done() {
			try {
				done.accept(get());
			} catch (InterruptedException | ExecutionException e) {
				error.accept(e);
			}
		}
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
	private SwingUI() {
		throw new UnsupportedOperationException();
	}

}
