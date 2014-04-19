
package net.sourceforge.filebot.ui;


import javax.swing.Icon;
import javax.swing.JComponent;


public interface PanelBuilder {
	
	public String getName();
	

	public Icon getIcon();
	

	public JComponent create();
	
}
