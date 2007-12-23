
package net.sourceforge.filebot.ui;


import java.awt.BorderLayout;

import javax.swing.Icon;
import javax.swing.JPanel;


public class FileBotPanel extends JPanel {
	
	private String text;
	
	private Icon icon;
	
	
	public FileBotPanel(String text, Icon icon) {
		super(new BorderLayout(10, 20));
		this.text = text;
		this.icon = icon;
	}
	

	public Icon getIcon() {
		return icon;
	}
	

	public String getText() {
		return text;
	}
	

	@Override
	public String toString() {
		return getText();
	}
	
}
