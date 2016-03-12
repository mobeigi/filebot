package net.filebot.ui;

import static java.awt.Cursor.*;
import static net.filebot.util.ui.SwingUI.*;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import net.filebot.ResourceManager;
import net.filebot.util.ui.DefaultFancyListCellRenderer;
import net.filebot.util.ui.SwingUI;
import net.filebot.web.SearchResult;
import net.miginfocom.swing.MigLayout;

public class SelectDialog<T> extends JDialog {

	private JLabel headerLabel = new JLabel();
	private JCheckBox autoRepeatCheckBox = new JCheckBox();

	private JList list;

	private Action selectedAction = null;

	public SelectDialog(Component parent, Collection<? extends T> options) {
		this(parent, options, false, false);
	}

	public SelectDialog(Component parent, Collection<? extends T> options, boolean autoRepeatEnabled, boolean autoRepeatSelected) {
		super(getWindow(parent), "Select", ModalityType.DOCUMENT_MODAL);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		// initialize list
		list = new JList(options.toArray());

		// select first element
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setSelectedIndex(0);

		DefaultFancyListCellRenderer renderer = new DefaultFancyListCellRenderer(4) {

			@Override
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				super.getListCellRendererComponent(list, convertValueToString(value), index, isSelected, cellHasFocus);
				configureValue(this, value);
				return this;
			}
		};

		renderer.setHighlightingEnabled(false);

		list.setCellRenderer(renderer);
		list.addMouseListener(mouseListener);

		JComponent c = (JComponent) getContentPane();

		c.setLayout(new MigLayout("insets 1.5mm 1.5mm 2.7mm 1.5mm, nogrid, fill", "", "[pref!][fill][pref!]"));

		c.add(headerLabel, "wmin 150px, wrap");
		c.add(new JScrollPane(list), "wmin 150px, hmin 150px, grow, wrap 2mm");

		c.add(new JButton(selectAction), "align center, id select");
		c.add(new JButton(cancelAction), "gap unrel, id cancel");

		// add repeat button
		if (autoRepeatEnabled) {
			autoRepeatCheckBox.setSelected(autoRepeatSelected);
			autoRepeatCheckBox.setToolTipText("Remember");
			autoRepeatCheckBox.setCursor(getPredefinedCursor(HAND_CURSOR));
			autoRepeatCheckBox.setIcon(ResourceManager.getIcon("button.repeat"));
			autoRepeatCheckBox.setSelectedIcon(ResourceManager.getIcon("button.repeat.selected"));
			c.add(autoRepeatCheckBox, "pos 1al select.y n select.y2");
		}

		// set default size and location
		setMinimumSize(new Dimension(220, 240));
		setSize(new Dimension(240, 260));

		// Shortcut Enter
		SwingUI.installAction(list, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), selectAction);
	}

	protected String convertValueToString(Object value) {
		return value.toString();
	}

	protected void configureValue(JComponent render, Object value) {
		if (value instanceof SearchResult) {
			render.setToolTipText(getSearchResultPopup((SearchResult) value));
		} else {
			render.setToolTipText(null);
		}
	}

	protected String getSearchResultPopup(SearchResult item) {
		StringBuilder html = new StringBuilder(64);
		html.append("<html><b>").append(escapeHTML(item.toString())).append("</b><br>");
		String[] names = item.getAliasNames();
		if (names.length > 0) {
			html.append("<br>AKA:<br>");
			for (String n : names) {
				html.append("• ").append(escapeHTML(n)).append("<br>");
			}
		}
		html.append("<br>ID: <br>• ").append(Integer.toString(item.getId())).append("</html>");
		return html.toString();
	}

	public JLabel getHeaderLabel() {
		return headerLabel;
	}

	public JCheckBox getAutoRepeatCheckBox() {
		return autoRepeatCheckBox;
	}

	public Action getSelectedAction() {
		return selectedAction;
	}

	@SuppressWarnings("unchecked")
	public T getSelectedValue() {
		if (selectedAction != selectAction)
			return null;

		return (T) list.getSelectedValue();
	}

	public void close() {
		setVisible(false);
		dispose();
	}

	public Action getSelectAction() {
		return selectAction;
	}

	public Action getCancelAction() {
		return cancelAction;
	}

	private final Action selectAction = new AbstractAction("Select", ResourceManager.getIcon("dialog.continue")) {

		@Override
		public void actionPerformed(ActionEvent e) {
			selectedAction = this;
			close();
		}
	};

	private final Action cancelAction = new AbstractAction("Cancel", ResourceManager.getIcon("dialog.cancel")) {

		@Override
		public void actionPerformed(ActionEvent e) {
			selectedAction = this;
			close();
		}
	};

	private final MouseAdapter mouseListener = new MouseAdapter() {

		@Override
		public void mouseClicked(MouseEvent e) {
			if (SwingUtilities.isLeftMouseButton(e) && (e.getClickCount() == 2)) {
				selectAction.actionPerformed(null);
			}
		}
	};

}
