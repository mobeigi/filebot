
package net.sourceforge.filebot.ui.sal;


import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;

import net.sourceforge.filebot.resources.ResourceManager;


public class SaveAction extends AbstractAction {
	
	protected Saveable saveable;
	
	
	public SaveAction(Saveable saveable) {
		super("Save as ...", ResourceManager.getIcon("action.save"));
		this.saveable = saveable;
	}
	

	protected void save(File file) {
		saveable.save(file);
	}
	

	protected String getDefaultFileName() {
		return saveable.getDefaultFileName();
	}
	

	protected boolean isSaveable() {
		return saveable.isSaveable();
	}
	

	public void actionPerformed(ActionEvent e) {
		if (!isSaveable())
			return;
		
		JFileChooser chooser = new JFileChooser();
		
		chooser.setMultiSelectionEnabled(false);
		chooser.setSelectedFile(new File(getDefaultFileName()));
		
		if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION)
			return;
		
		save(chooser.getSelectedFile());
	}
	
}
