
package net.sourceforge.filebot.ui.analyze;


import javax.swing.Icon;
import javax.swing.JComponent;

import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.ui.PanelBuilder;


public class AnalyzePanelBuilder implements PanelBuilder {
	
	@Override
	public String getName() {
		return "Analyze";
	}
	

	@Override
	public Icon getIcon() {
		return ResourceManager.getIcon("panel.analyze");
	}
	

	@Override
	public JComponent create() {
		return new AnalyzePanel();
	}
	
}
