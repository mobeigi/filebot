
package net.sourceforge.filebot.ui.panel.list;


import java.awt.BorderLayout;

import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.filebot.ui.FileBotPanel;


public class ListPanel extends FileBotPanel {
	
	private FileList fileList = new FileList();
	
	
	public ListPanel() {
		super("List", ResourceManager.getIcon("panel.list"));
		add(fileList, BorderLayout.CENTER);
	}
}
