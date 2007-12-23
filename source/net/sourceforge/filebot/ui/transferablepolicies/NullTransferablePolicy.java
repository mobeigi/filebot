
package net.sourceforge.filebot.ui.transferablepolicies;


import java.awt.datatransfer.Transferable;


public class NullTransferablePolicy extends TransferablePolicy {
	
	public boolean accept(Transferable tr) {
		return false;
	}
	

	public boolean handleTransferable(Transferable tr, boolean add) {
		return false;
	}
	

	@Override
	public String getDescription() {
		return null;
	}
	
}
