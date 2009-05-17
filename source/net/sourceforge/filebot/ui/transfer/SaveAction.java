
package net.sourceforge.filebot.ui.transfer;


import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JFileChooser;

import net.sourceforge.filebot.FileBotUtilities;
import net.sourceforge.filebot.ResourceManager;


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
		return null;
	}
	

	public void actionPerformed(ActionEvent evt) {
		if (!canExport())
			return;
		
		JFileChooser chooser = new JFileChooser();
		
		chooser.setMultiSelectionEnabled(false);
		
		chooser.setSelectedFile(new File(getDefaultFolder(), FileBotUtilities.validateFileName(getDefaultFileName())));
		
		if (chooser.showSaveDialog((JComponent) evt.getSource()) != JFileChooser.APPROVE_OPTION)
			return;
		
		try {
			export(chooser.getSelectedFile());
		} catch (IOException e) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, e.toString(), e);
		}
	}
}
