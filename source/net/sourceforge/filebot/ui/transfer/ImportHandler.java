
package net.sourceforge.filebot.ui.transfer;


import javax.swing.TransferHandler.TransferSupport;


public interface ImportHandler {
	
	public abstract boolean canImport(TransferSupport support);
	

	public abstract boolean importData(TransferSupport support);
	
}
