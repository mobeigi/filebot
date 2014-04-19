
package net.sourceforge.filebot.ui.rename;


import javax.swing.Icon;
import javax.swing.JComponent;

import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.ui.PanelBuilder;


public class RenamePanelBuilder implements PanelBuilder {
	
	@Override
	public String getName() {
		return "Rename";
	}
	

	@Override
	public Icon getIcon() {
		return ResourceManager.getIcon("panel.rename");
	}
	

	@Override
	public JComponent create() {
		return new RenamePanel();
	}
	
}
