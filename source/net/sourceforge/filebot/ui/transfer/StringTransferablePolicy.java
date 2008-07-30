
package net.sourceforge.filebot.ui.transfer;


import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;


public abstract class StringTransferablePolicy extends TransferablePolicy {
	
	@Override
	public boolean accept(Transferable tr) {
		return tr.isDataFlavorSupported(DataFlavor.stringFlavor);
	}
	

	@Override
	public void handleTransferable(Transferable tr, TransferAction action) {
		String string;
		
		try {
			string = (String) tr.getTransferData(DataFlavor.stringFlavor);
		} catch (UnsupportedFlavorException e) {
			// should no happen
			throw new RuntimeException(e);
		} catch (IOException e) {
			// should no happen
			throw new RuntimeException(e);
		}
		
		if (action != TransferAction.ADD)
			clear();
		
		load(string);
	}
	

	protected void clear() {
		
	}
	

	protected abstract void load(String string);
	
}
