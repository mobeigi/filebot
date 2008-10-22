
package net.sourceforge.filebot.ui;


import javax.swing.Icon;
import javax.swing.JPanel;

import net.sourceforge.tuned.MessageHandler;


public class FileBotPanel extends JPanel {
	
	private final String name;
	private final Icon icon;
	
	
	public FileBotPanel(String title, Icon icon) {
		super(null);
		
		this.name = title;
		this.icon = icon;
	}
	

	public Icon getIcon() {
		return icon;
	}
	

	public String getPanelName() {
		return name;
	}
	

	public MessageHandler getMessageHandler() {
		return null;
	}
	
}
