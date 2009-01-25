
package net.sourceforge.filebot.ui;


import javax.swing.Icon;
import javax.swing.JComponent;

import net.sourceforge.tuned.MessageHandler;


public class FileBotPanel extends JComponent {
	
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
	

	public MessageHandler getMessageHandler() {
		return null;
	}
	

	@Override
	public String toString() {
		return getPanelName();
	}
	
}
