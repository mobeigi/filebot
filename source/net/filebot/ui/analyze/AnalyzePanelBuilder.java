
package net.filebot.ui.analyze;


import javax.swing.Icon;
import javax.swing.JComponent;

import net.filebot.ResourceManager;
import net.filebot.ui.PanelBuilder;


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
