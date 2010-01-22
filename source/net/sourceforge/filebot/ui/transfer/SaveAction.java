
package net.sourceforge.filebot.ui.transfer;


import static net.sourceforge.tuned.FileUtilities.*;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JFileChooser;

import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.Settings;


public class SaveAction extends AbstractAction {
	
	public static final String EXPORT_HANDLER = "exportHandler";
	

	public SaveAction(FileExportHandler exportHandler) {
		this("Save as ...", ResourceManager.getIcon("action.save"), exportHandler);
	}
	

	public SaveAction(String name, Icon icon, FileExportHandler exportHandler) {
		putValue(NAME, name);
		putValue(SMALL_ICON, icon);
		putValue(EXPORT_HANDLER, exportHandler);
	}
	

	public FileExportHandler getExportHandler() {
		return (FileExportHandler) getValue(EXPORT_HANDLER);
	}
	

	protected boolean canExport() {
		return getExportHandler().canExport();
	}
	

	protected void export(File file) throws IOException {
		getExportHandler().export(file);
	}
	

	protected String getDefaultFileName() {
		return getExportHandler().getDefaultFileName();
	}
	

	protected File getDefaultFolder() {
		String lastLocation = Settings.forPackage(SaveAction.class).get("save.location");
		
		if (lastLocation == null || lastLocation.isEmpty())
			return null;
		
		return new File(lastLocation);
	}
	

	public void actionPerformed(ActionEvent evt) {
		if (!canExport())
			return;
		
		JFileChooser chooser = new JFileChooser();
		
		chooser.setMultiSelectionEnabled(false);
		
		chooser.setSelectedFile(new File(getDefaultFolder(), validateFileName(getDefaultFileName())));
		
		if (chooser.showSaveDialog((JComponent) evt.getSource()) != JFileChooser.APPROVE_OPTION)
			return;
		
		try {
			export(chooser.getSelectedFile());
		} catch (IOException e) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, e.toString(), e);
		}
		
		// remember last location
		Settings.forPackage(SaveAction.class).put("save.location", chooser.getCurrentDirectory().getPath());
	}
}
