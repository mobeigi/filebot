
package net.filebot.ui.list;


import javax.swing.Icon;
import javax.swing.JComponent;

import net.filebot.ResourceManager;
import net.filebot.ui.PanelBuilder;


public class ListPanelBuilder implements PanelBuilder {
	
	@Override
	public String getName() {
		return "List";
	}
	

	@Override
	public Icon getIcon() {
		return ResourceManager.getIcon("panel.list");
	}
	

	@Override
	public JComponent create() {
		return new ListPanel();
	}
	
}
