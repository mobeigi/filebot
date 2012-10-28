
package net.sourceforge.filebot.ui;


import static net.sourceforge.tuned.ui.TunedUtilities.*;

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
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.tuned.ui.DefaultFancyListCellRenderer;
import net.sourceforge.tuned.ui.TunedUtilities;


public class SelectDialog<T> extends JDialog {
	
	private final JLabel headerLabel = new JLabel();
	
	private final JList list;
	
	private boolean valueSelected = false;
	
	
	public SelectDialog(Component parent, Collection<? extends T> options) {
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
				return super.getListCellRendererComponent(list, convertValueToString(value), index, isSelected, cellHasFocus);
			}
		};
		
		renderer.setHighlightingEnabled(false);
		
		list.setCellRenderer(renderer);
		list.addMouseListener(mouseListener);
		
		JComponent c = (JComponent) getContentPane();
		
		c.setLayout(new MigLayout("insets 1.5mm 1.5mm 2.7mm 1.5mm, nogrid, fill", "", "[pref!][fill][pref!]"));
		
		c.add(headerLabel, "wrap");
		c.add(new JScrollPane(list), "grow, wrap 2mm");
		
		c.add(new JButton(selectAction), "align center, id select");
		c.add(new JButton(cancelAction), "gap unrel, id cancel");
		
		// set default size and location
		setSize(new Dimension(210, 210));
		
		// Shortcut Enter
		TunedUtilities.installAction(list, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), selectAction);
	}
	
	
	protected String convertValueToString(Object value) {
		return value.toString();
	}
	
	
	public JLabel getHeaderLabel() {
		return headerLabel;
	}
	
	
	@SuppressWarnings("unchecked")
	public T getSelectedValue() {
		if (!valueSelected)
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
			valueSelected = true;
			close();
		}
	};
	
	private final Action cancelAction = new AbstractAction("Cancel", ResourceManager.getIcon("dialog.cancel")) {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			valueSelected = false;
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
