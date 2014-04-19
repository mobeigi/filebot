
package net.sourceforge.filebot.ui.sfv;


import javax.swing.Icon;
import javax.swing.JComponent;

import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.ui.PanelBuilder;


public class SfvPanelBuilder implements PanelBuilder {
	
	@Override
	public String getName() {
		return "SFV";
	}
	

	@Override
	public Icon getIcon() {
		return ResourceManager.getIcon("panel.sfv");
	}
	

	@Override
	public JComponent create() {
		return new SfvPanel();
	}
	
}
