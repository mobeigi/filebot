
package net.sourceforge.filebot.ui;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import net.sourceforge.filebot.resources.ResourceManager;


public class FileBotTabComponent extends JPanel {
	
	private final JLabel label;
	
	
	public FileBotTabComponent() {
		this("", null);
	}
	

	public FileBotTabComponent(String title, Icon icon) {
		super(new BorderLayout(0, 0));
		setOpaque(false);
		
		label = new JLabel(title, SwingConstants.LEFT);
		label.setIcon(icon);
		
		label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 2));
		
		add(label, BorderLayout.CENTER);
		add(new CloseButton(tabCloseAction), BorderLayout.EAST);
	}
	

	public void setIcon(Icon icon) {
		label.setIcon(icon);
	}
	

	public void setText(String text) {
		label.setText(text);
	}
	

	public String getText() {
		return label.getText();
	}
	

	public void close() {
		JTabbedPane tabs = (JTabbedPane) SwingUtilities.getAncestorOfClass(JTabbedPane.class, FileBotTabComponent.this);
		tabs.removeTabAt(tabs.indexOfTabComponent(FileBotTabComponent.this));
	}
	
	private final AbstractAction tabCloseAction = new AbstractAction(null, null) {
		
		public void actionPerformed(ActionEvent e) {
			close();
		}
	};
	
	
	private class CloseButton extends JButton {
		
		public CloseButton(AbstractAction action) {
			super(action);
			
			setContentAreaFilled(false);
			setBorderPainted(false);
			setFocusable(false);
			
			setIcon(ResourceManager.getIcon("tab.close"));
			setRolloverIcon(ResourceManager.getIcon("tab.close.hover"));
			setRolloverEnabled(true);
			
			setPreferredSize(new Dimension(17, 17));
		}
	};
	
}
