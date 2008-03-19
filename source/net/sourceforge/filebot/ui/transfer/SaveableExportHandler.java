
package net.sourceforge.filebot.ui.transfer;


import java.awt.datatransfer.Transferable;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.TransferHandler;

import net.sourceforge.filebot.FileBotUtil;
import net.sourceforge.filebot.Settings;
import net.sourceforge.tuned.TemporaryFolder;


public class SaveableExportHandler implements ExportHandler {
	
	private final Saveable saveable;
	
	
	public SaveableExportHandler(Saveable saveable) {
		this.saveable = saveable;
	}
	

	@Override
	public int getSourceActions(JComponent c) {
		if ((saveable == null) || !saveable.isSaveable())
			return TransferHandler.NONE;
		
		return TransferHandler.MOVE | TransferHandler.COPY;
	}
	

	@Override
	public Transferable createTransferable(JComponent c) {
		try {
			// Remove invalid characters from default filename
			String name = FileBotUtil.validateFileName(saveable.getDefaultFileName());
			
			File temporaryFile = TemporaryFolder.getFolder(Settings.ROOT).createFile(name);
			saveable.save(temporaryFile);
			
			return new FileTransferable(temporaryFile);
		} catch (IOException e) {
			// should not happen
			Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, e.toString(), e);
		}
		
		return null;
	}
	

	@Override
	public void exportDone(JComponent source, Transferable data, int action) {
		
	}
}
