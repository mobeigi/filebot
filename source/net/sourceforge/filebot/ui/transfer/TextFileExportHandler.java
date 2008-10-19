
package net.sourceforge.filebot.ui.transfer;


import java.awt.datatransfer.Transferable;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JComponent;
import javax.swing.TransferHandler;


public abstract class TextFileExportHandler implements TransferableExportHandler, FileExportHandler {
	
	public abstract boolean canExport();
	

	public abstract void export(PrintWriter out);
	

	public abstract String getDefaultFileName();
	

	@Override
	public void export(File file) throws IOException {
		PrintWriter out = new PrintWriter(file, "UTF-8");
		
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
		// get transfer data
		StringWriter buffer = new StringWriter();
		export(new PrintWriter(buffer));
		
		return new LazyTextFileTransferable(buffer.toString(), getDefaultFileName());
	}
	

	@Override
	public void exportDone(JComponent source, Transferable data, int action) {
		
	}
	
}
