
package net.sourceforge.filebot.ui.transfer;


import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;

import javax.swing.JComponent;
import javax.swing.TransferHandler;


public class DefaultTransferHandler extends TransferHandler {
	
	private ImportHandler importHandler;
	private ExportHandler exportHandler;
	private ClipboardHandler clipboardHandler;
	
	private boolean dragging = false;
	
	
	public DefaultTransferHandler(ImportHandler importHandler, ExportHandler exportHandler) {
		this(importHandler, exportHandler, new DefaultClipboardHandler());
	}
	

	public DefaultTransferHandler(ImportHandler importHandler, ExportHandler exportHandler, ClipboardHandler clipboardHandler) {
		this.importHandler = importHandler;
		this.exportHandler = exportHandler;
		this.clipboardHandler = clipboardHandler;
	}
	

	@Override
	public boolean canImport(TransferSupport support) {
		// show "drop allowed" cursor when dragging even though drop is not allowed
		if (dragging)
			return true;
		
		if (importHandler != null)
			return importHandler.canImport(support);
		
		return false;
	}
	

	@Override
	public boolean importData(TransferSupport support) {
		if (dragging)
			return false;
		
		if (!canImport(support))
			return false;
		
		return importHandler.importData(support);
	}
	

	@Override
	protected void exportDone(JComponent source, Transferable data, int action) {
		dragging = false;
		
		if (data == null)
			return;
		
		if (exportHandler != null)
			exportHandler.exportDone(source, data, action);
	}
	

	@Override
	public int getSourceActions(JComponent c) {
		if (exportHandler != null)
			return exportHandler.getSourceActions(c);
		
		return NONE;
	}
	

	@Override
	protected Transferable createTransferable(JComponent c) {
		dragging = true;
		
		if (exportHandler != null)
			return exportHandler.createTransferable(c);
		
		return null;
	}
	

	@Override
	public void exportToClipboard(JComponent comp, Clipboard clip, int action) throws IllegalStateException {
		if (clipboardHandler != null)
			clipboardHandler.exportToClipboard(comp, clip, action);
	}
	

	public ImportHandler getImportHandler() {
		return importHandler;
	}
	

	public void setImportHandler(ImportHandler importHandler) {
		this.importHandler = importHandler;
	}
	

	public ExportHandler getExportHandler() {
		return exportHandler;
	}
	

	public void setExportHandler(ExportHandler exportHandler) {
		this.exportHandler = exportHandler;
	}
	

	public ClipboardHandler getClipboardHandler() {
		return clipboardHandler;
	}
	

	public void setClipboardHandler(ClipboardHandler clipboardHandler) {
		this.clipboardHandler = clipboardHandler;
	}
	
}
