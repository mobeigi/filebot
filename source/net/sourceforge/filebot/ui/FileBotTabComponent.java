
package net.sourceforge.filebot.ui;


import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sourceforge.filebot.resources.ResourceManager;


public class FileBotTabComponent extends JPanel {
	
	private final JLabel label = new JLabel();
	private final JButton closeButton = createCloseButton();
	
	
	public FileBotTabComponent() {
		super(new BorderLayout(0, 0));
		setOpaque(false);
		
		label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 2));
		
		add(label, BorderLayout.CENTER);
		add(closeButton, BorderLayout.EAST);
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
	

	public JButton getCloseButton() {
		return closeButton;
	}
	

	private JButton createCloseButton() {
		JButton button = new JButton();
		
		button.setContentAreaFilled(false);
		button.setBorderPainted(false);
		button.setFocusable(false);
		button.setRolloverEnabled(true);
		
		button.setIcon(ResourceManager.getIcon("tab.close"));
		button.setRolloverIcon(ResourceManager.getIcon("tab.close.hover"));
		
		button.setPreferredSize(new Dimension(17, 17));
		
		return button;
	}
	
}
