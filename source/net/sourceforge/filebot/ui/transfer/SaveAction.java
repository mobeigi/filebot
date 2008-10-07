
package net.sourceforge.filebot.ui.transfer;


import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFileChooser;

import net.sourceforge.filebot.FileBotUtil;
import net.sourceforge.filebot.resources.ResourceManager;


public class SaveAction extends AbstractAction {
	
	private final FileExportHandler exportHandler;
	
	
	public SaveAction(FileExportHandler exportHandler) {
		super("Save as ...", ResourceManager.getIcon("action.save"));
		this.exportHandler = exportHandler;
	}
	

	public FileExportHandler getExportHandler() {
		return exportHandler;
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
		
		chooser.setSelectedFile(new File(getDefaultFolder(), FileBotUtil.validateFileName(getDefaultFileName())));
		
		if (chooser.showSaveDialog((JComponent) evt.getSource()) != JFileChooser.APPROVE_OPTION)
			return;
		
		try {
			export(chooser.getSelectedFile());
		} catch (IOException e) {
			Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, e.toString(), e);
		}
	}
}
