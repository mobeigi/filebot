
package net.sourceforge.filebot.ui.list;


import javax.swing.Icon;
import javax.swing.JComponent;

import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.ui.PanelBuilder;


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
