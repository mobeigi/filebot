
package net.sourceforge.filebot.ui.panel.sfv;


import java.awt.BorderLayout;

import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.filebot.ui.FileBotPanel;


public class SfvPanel extends FileBotPanel {
	
	public SfvPanel() {
		super("SFV", ResourceManager.getIcon("panel.sfv"));
		add(new SfvTablePanel(), BorderLayout.CENTER);
	}
	
}
