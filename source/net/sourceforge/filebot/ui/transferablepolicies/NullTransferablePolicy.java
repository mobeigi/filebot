
package net.sourceforge.filebot.ui.transferablepolicies;


import java.awt.datatransfer.Transferable;


public class NullTransferablePolicy extends TransferablePolicy {
	
	@Override
	public boolean accept(Transferable tr) {
		return false;
	}
	

	@Override
	public void handleTransferable(Transferable tr, boolean add) {
		
	}
	

	@Override
	public String getDescription() {
		return null;
	}
	
}
