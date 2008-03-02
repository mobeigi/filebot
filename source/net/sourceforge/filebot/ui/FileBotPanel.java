
package net.sourceforge.filebot.ui;


import java.awt.BorderLayout;

import javax.swing.Icon;
import javax.swing.JPanel;


public class FileBotPanel extends JPanel {
	
	private String title;
	private Icon icon;
	
	
	public FileBotPanel(String title, Icon icon) {
		super(new BorderLayout(10, 0));
		this.title = title;
		this.icon = icon;
	}
	

	public Icon getIcon() {
		return icon;
	}
	

	public String getTitle() {
		return title;
	}
	
}
