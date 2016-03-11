
package net.filebot.ui.analyze;

import javax.swing.Icon;
import javax.swing.JComponent;

import net.filebot.ResourceManager;
import net.filebot.ui.PanelBuilder;

public class AnalyzePanelBuilder implements PanelBuilder {

	@Override
	public String getName() {
		return "Filter";
	}

	@Override
	public Icon getIcon() {
		return ResourceManager.getIcon("panel.analyze");
	}

	@Override
	public JComponent create() {
		AnalyzePanel panel = new AnalyzePanel();
		panel.addTool(new ExtractTool());
		panel.addTool(new TypeTool());
		panel.addTool(new SplitTool());
		panel.addTool(new AttributeTool());
		panel.addTool(new MediaInfoTool());
		return panel;
	}

}
