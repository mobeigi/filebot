
package net.sourceforge.filebot.ui.subtitle;


import javax.swing.Icon;
import javax.swing.JComponent;

import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.ui.PanelBuilder;


public class SubtitlePanelBuilder implements PanelBuilder {
	
	@Override
	public String getName() {
		return "Subtitles";
	}
	

	@Override
	public Icon getIcon() {
		return ResourceManager.getIcon("panel.subtitle");
	}
	

	@Override
	public JComponent create() {
		return new SubtitlePanel();
	}
	
}
