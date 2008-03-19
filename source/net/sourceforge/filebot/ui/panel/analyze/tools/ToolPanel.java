
package net.sourceforge.filebot.ui.panel.analyze.tools;


import java.io.File;
import java.util.Collection;

import javax.swing.JComponent;


public abstract class ToolPanel extends JComponent {
	
	private final String name;
	
	
	public ToolPanel(String name) {
		this.name = name;
	}
	

	public String getToolName() {
		return name;
	}
	

	public abstract void update(Collection<File> list);
}
