
package net.sourceforge.filebot.ui.transferablepolicies;


import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;


public abstract class TextTransferablePolicy extends TransferablePolicy {
	
	public boolean accept(Transferable tr) {
		if (!isEnabled())
			return false;
		
		return tr.isDataFlavorSupported(DataFlavor.stringFlavor);
	}
	

	public boolean handleTransferable(Transferable tr, boolean add) {
		try {
			String string = (String) tr.getTransferData(DataFlavor.stringFlavor);
			
			if (!add) {
				clear();
			}
			
			return load(string);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	

	protected abstract void clear();
	

	protected abstract boolean load(String text);
	
}
