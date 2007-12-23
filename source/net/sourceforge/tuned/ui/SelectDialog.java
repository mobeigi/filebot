
package net.sourceforge.tuned.ui;


import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;


public class SelectDialog<T> extends JDialog {
	
	private Collection<T> options;
	
	private Map<T, ? extends Icon> iconMap;
	
	
	public SelectDialog(Window owner, Collection<T> options, Map<T, ? extends Icon> icons) {
		this(owner);
		this.options = options;
		this.iconMap = icons;
		initialize();
	}
	

	public SelectDialog(Window owner, Collection<T> options) {
		this(owner, options, null);
	}
	

	private SelectDialog(Window owner) {
		super(owner, "Select", ModalityType.DOCUMENT_MODAL);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		list.setCellRenderer(cellRenderer);
		list.addMouseListener(mouseListener);
		
		label.setText("Select:");
		
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
		
		setMinimumSize(new Dimension(175, 175));
		setSize(new Dimension(200, 190));
		
		// Shortcut Enter
		Integer actionMapKey = new Integer(selectAction.hashCode());
		list.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released ENTER"), actionMapKey);
		list.getActionMap().put(actionMapKey, selectAction);
		
		// Shortcut Escape
		actionMapKey = new Integer(cancelAction.hashCode());
		list.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released ESCAPE"), actionMapKey);
		list.getActionMap().put(actionMapKey, cancelAction);
	}
	

	public void setDefaultLocation() {
		Point p = getOwner().getLocation();
		Dimension d = getOwner().getSize();
		
		Point offset = new Point(d.width / 4, d.height / 7);
		setLocation(p.x + offset.x, p.y + offset.y);
	}
	

	private void initialize() {
		DefaultListModel model = new DefaultListModel();
		
		for (Object e : options) {
			model.addElement(e);
		}
		
		list.setModel(model);
		list.setSelectedIndex(0);
		
		setDefaultLocation();
	}
	
	private JLabel label = new JLabel();
	
	private JList list = new JList();
	
	private T selectedValue = null;
	
	
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
	
	private AbstractAction selectAction = new AbstractAction("Select") {
		
		@SuppressWarnings("unchecked")
		public void actionPerformed(ActionEvent e) {
			selectedValue = (T) list.getSelectedValue();
			setVisible(false);
			dispose();
		}
	};
	
	private AbstractAction cancelAction = new AbstractAction("Cancel") {
		
		public void actionPerformed(ActionEvent e) {
			selectedValue = null;
			setVisible(false);
			dispose();
		}
	};
	
	
	protected String convertValueToString(Object value) {
		return value.toString();
	}
	
	private DefaultListCellRenderer cellRenderer = new DefaultListCellRenderer() {
		
		private Border border = BorderFactory.createEmptyBorder(4, 4, 4, 4);
		
		
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			super.getListCellRendererComponent(list, convertValueToString(value), index, isSelected, cellHasFocus);
			setBorder(border);
			
			if (iconMap != null)
				setIcon(iconMap.get(value));
			
			return this;
		}
	};
	
	private MouseAdapter mouseListener = new MouseAdapter() {
		
		@Override
		public void mouseClicked(MouseEvent e) {
			if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2)
				selectAction.actionPerformed(null);
		}
	};
}
