
package net.sourceforge.filebot.ui;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.tuned.ui.DefaultFancyListCellRenderer;
import net.sourceforge.tuned.ui.SimpleListModel;
import net.sourceforge.tuned.ui.TunedUtil;


public class SelectDialog<T> extends JDialog {
	
	private JLabel label = new JLabel();
	
	private JList list = new JList();
	
	private T selectedValue = null;
	
	
	public SelectDialog(Window owner, Collection<T> options) {
		super(owner, "Select", ModalityType.DOCUMENT_MODAL);
		
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		list.setCellRenderer(new SelectListCellRenderer());
		list.addMouseListener(mouseListener);
		
		setText("Select:");
		
		JComponent c = (JComponent) getContentPane();
		
		int border = 5;
		c.setBorder(BorderFactory.createEmptyBorder(border, border, border, border));
		c.setLayout(new BorderLayout(border, border));
		
		JPanel listPanel = new JPanel(new BorderLayout());
		
		listPanel.add(new JScrollPane(list), BorderLayout.CENTER);
		
		Box buttonBox = Box.createHorizontalBox();
		buttonBox.setBorder(new EmptyBorder(5, 5, 5, 5));
		
		buttonBox.add(Box.createHorizontalGlue());
		buttonBox.add(new JButton(selectAction));
		buttonBox.add(Box.createHorizontalStrut(10));
		buttonBox.add(new JButton(cancelAction));
		buttonBox.add(Box.createHorizontalGlue());
		
		c.add(label, BorderLayout.NORTH);
		c.add(listPanel, BorderLayout.CENTER);
		c.add(buttonBox, BorderLayout.SOUTH);
		
		// bounds and  location
		setMinimumSize(new Dimension(175, 175));
		setSize(new Dimension(200, 190));
		setLocation(TunedUtil.getPreferredLocation(this));
		
		// default selection
		list.setModel(new SimpleListModel(options));
		list.setSelectedIndex(0);
		
		// Shortcut Enter
		Integer actionMapKey = new Integer(selectAction.hashCode());
		list.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released ENTER"), actionMapKey);
		list.getActionMap().put(actionMapKey, selectAction);
		
		// Shortcut Escape
		actionMapKey = new Integer(cancelAction.hashCode());
		list.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released ESCAPE"), actionMapKey);
		list.getActionMap().put(actionMapKey, cancelAction);
	}
	

	public void setText(String s) {
		label.setText(s);
	}
	

	public T getSelectedValue() {
		return selectedValue;
	}
	

	public void setSelectedValue(T value) {
		this.selectedValue = value;
		list.setSelectedValue(value, true);
	}
	
	private AbstractAction selectAction = new AbstractAction("Select", ResourceManager.getIcon("dialog.continue")) {
		
		@SuppressWarnings("unchecked")
		public void actionPerformed(ActionEvent e) {
			selectedValue = (T) list.getSelectedValue();
			setVisible(false);
			dispose();
		}
	};
	
	private AbstractAction cancelAction = new AbstractAction("Cancel", ResourceManager.getIcon("dialog.cancel")) {
		
		public void actionPerformed(ActionEvent e) {
			selectedValue = null;
			setVisible(false);
			dispose();
		}
	};
	
	
	protected String convertValueToString(Object value) {
		return value.toString();
	}
	
	
	private class SelectListCellRenderer extends DefaultFancyListCellRenderer {
		
		public SelectListCellRenderer() {
			super(4);
			setHighlightingEnabled(false);
		}
		

		@Override
		public void configureListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			super.configureListCellRendererComponent(list, convertValueToString(value), index, isSelected, cellHasFocus);
		}
	};
	
	private MouseAdapter mouseListener = new MouseAdapter() {
		
		@Override
		public void mouseClicked(MouseEvent e) {
			if (SwingUtilities.isLeftMouseButton(e) && (e.getClickCount() == 2))
				selectAction.actionPerformed(null);
		}
	};
}
