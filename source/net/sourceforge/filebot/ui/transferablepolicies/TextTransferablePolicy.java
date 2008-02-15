
package net.sourceforge.filebot.ui.transferablepolicies;


import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;


public abstract class TextTransferablePolicy extends TransferablePolicy {
	
	@Override
	public boolean accept(Transferable tr) {
		if (!isEnabled())
			return false;
		
		return tr.isDataFlavorSupported(DataFlavor.stringFlavor);
	}
	

	@Override
	public void handleTransferable(Transferable tr, boolean add) {
		try {
			String string = (String) tr.getTransferData(DataFlavor.stringFlavor);
			
			if (!add) {
				clear();
			}
			
			load(string);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	protected abstract void clear();
	

	protected abstract boolean load(String text);
	
}
