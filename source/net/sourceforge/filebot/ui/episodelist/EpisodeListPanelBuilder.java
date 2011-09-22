
package net.sourceforge.filebot.ui.episodelist;


import javax.swing.Icon;
import javax.swing.JComponent;

import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.ui.PanelBuilder;


public class EpisodeListPanelBuilder implements PanelBuilder {
	
	@Override
	public String getName() {
		return "Episodes";
	}
	

	@Override
	public Icon getIcon() {
		return ResourceManager.getIcon("panel.episodelist");
	}
	

	@Override
	public JComponent create() {
		return new EpisodeListPanel();
	}
	
}
