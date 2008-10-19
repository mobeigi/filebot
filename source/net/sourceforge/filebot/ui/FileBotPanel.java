
package net.sourceforge.filebot.ui;


import javax.swing.Icon;
import javax.swing.JPanel;


public class FileBotPanel extends JPanel {
	
	private final String name;
	private final Icon icon;
	
	
	public FileBotPanel(String title, Icon icon) {
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
