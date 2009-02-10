
package net.sourceforge.filebot.ui;


import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.tuned.ui.ArrayListModel;
import net.sourceforge.tuned.ui.DefaultFancyListCellRenderer;
import net.sourceforge.tuned.ui.TunedUtilities;


public class SelectDialog<T> extends JDialog {
	
	private final JLabel headerLabel = new JLabel();
	
	private final JList list;
	
	private boolean valueSelected = false;
	
	
	public SelectDialog(Window owner, Collection<? extends T> options) {
		super(owner, "Select", ModalityType.DOCUMENT_MODAL);
		
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		// initialize list and select first element
		list = new JList(new ArrayListModel(options));
		list.setSelectedIndex(0);
		
		list.setCellRenderer(new SelectListCellRenderer());
		list.addMouseListener(mouseListener);
		
		JComponent c = (JComponent) getContentPane();
		
		c.setLayout(new MigLayout("insets 1.5mm, nogrid, fill"));
		
		c.add(headerLabel, "wrap");
		c.add(new JScrollPane(list), "grow, wrap 2mm");
		
		c.add(new JButton(selectAction), "align center");
		c.add(new JButton(cancelAction), "gap unrel, wrap 1.2mm");
		
		// set default size and location
		setSize(new Dimension(210, 210));
		setLocation(TunedUtilities.getPreferredLocation(this));
		
		// Shortcut Enter
		TunedUtilities.putActionForKeystroke(list, KeyStroke.getKeyStroke("released ENTER"), selectAction);
		
		// Shortcut Escape
		TunedUtilities.putActionForKeystroke(list, KeyStroke.getKeyStroke("released ESCAPE"), cancelAction);
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
	
	private AbstractAction selectAction = new AbstractAction("Select", ResourceManager.getIcon("dialog.continue")) {
		
		public void actionPerformed(ActionEvent e) {
			valueSelected = true;
			close();
		}
	};
	
	private AbstractAction cancelAction = new AbstractAction("Cancel", ResourceManager.getIcon("dialog.cancel")) {
		
		public void actionPerformed(ActionEvent e) {
			valueSelected = false;
			close();
		}
	};
	
	private MouseAdapter mouseListener = new MouseAdapter() {
		
		@Override
		public void mouseClicked(MouseEvent e) {
			if (SwingUtilities.isLeftMouseButton(e) && (e.getClickCount() == 2)) {
				selectAction.actionPerformed(null);
			}
		}
	};
	
	
	protected class SelectListCellRenderer extends DefaultFancyListCellRenderer {
		
		public SelectListCellRenderer() {
			super(4);
			setHighlightingEnabled(false);
		}
		

		@Override
		public void configureListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			super.configureListCellRendererComponent(list, convertValueToString(value), index, isSelected, cellHasFocus);
		}
	};
	
}
