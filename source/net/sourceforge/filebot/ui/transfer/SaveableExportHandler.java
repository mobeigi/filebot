
package net.sourceforge.filebot.ui.transfer;


import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.TransferHandler;


public class SaveableExportHandler implements ExportHandler {
	
	private Saveable saveable;
	
	private String tmpdir = System.getProperty("java.io.tmpdir");
	
	
	public SaveableExportHandler(Saveable saveable) {
		this.saveable = saveable;
	}
	

	@Override
	public void exportDone(JComponent source, Transferable data, int action) {
		try {
			List<?> list = (List<?>) data.getTransferData(DataFlavor.javaFileListFlavor);
			
			for (Object object : list) {
				File temporaryFile = (File) object;
				temporaryFile.deleteOnExit();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	@Override
	public int getSourceActions(JComponent c) {
		if (saveable == null || !saveable.isSaveable())
			return TransferHandler.NONE;
		
		return TransferHandler.MOVE | TransferHandler.COPY;
	}
	

	@Override
	public Transferable createTransferable(JComponent c) {
		try {
			File temporaryFile = new File(tmpdir, saveable.getDefaultFileName());
			temporaryFile.createNewFile();
			
			saveable.save(temporaryFile);
			return new FileTransferable(temporaryFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
}
