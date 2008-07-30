
package net.sourceforge.filebot.ui.transfer;


import java.awt.datatransfer.Transferable;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.TransferHandler;

import net.sourceforge.filebot.FileBotUtil;
import net.sourceforge.filebot.Settings;
import net.sourceforge.tuned.TemporaryFolder;


public abstract class FileExportHandler implements ExportHandler {
	
	public abstract boolean canExport();
	

	public abstract void export(OutputStream out) throws IOException;
	

	public abstract String getDefaultFileName();
	

	public void export(File file) throws IOException {
		OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
		
		try {
			export(out);
		} finally {
			out.close();
		}
	}
	

	@Override
	public int getSourceActions(JComponent c) {
		if (!canExport())
			return TransferHandler.NONE;
		
		return TransferHandler.MOVE | TransferHandler.COPY;
	}
	

	@Override
	public Transferable createTransferable(JComponent c) {
		try {
			// remove invalid characters from file name
			String name = FileBotUtil.validateFileName(getDefaultFileName());
			
			File temporaryFile = TemporaryFolder.getFolder(Settings.ROOT).createFile(name);
			
			export(temporaryFile);
			
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
