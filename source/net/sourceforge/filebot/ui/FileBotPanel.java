
package net.sourceforge.filebot.ui;


import java.awt.BorderLayout;

import javax.swing.Icon;
import javax.swing.JPanel;


public class FileBotPanel extends JPanel {
	
	private final String name;
	private final Icon icon;
	
	
	public FileBotPanel(String title, Icon icon) {
		super(new BorderLayout(10, 10));
		this.name = title;
		this.icon = icon;
	}
	

	public Icon getIcon() {
		return icon;
	}
	

	public String getPanelName() {
		return name;
	}
}
