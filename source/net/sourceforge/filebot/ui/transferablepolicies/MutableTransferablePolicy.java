
package net.sourceforge.filebot.ui.transferablepolicies;


import java.awt.datatransfer.Transferable;


public class MutableTransferablePolicy implements TransferablePolicy {
	
	private TransferablePolicy transferablePolicy = null;
	
	
	public void setTransferablePolicy(TransferablePolicy transferablePolicy) {
		this.transferablePolicy = transferablePolicy;
	}
	

	@Override
	public boolean accept(Transferable tr) {
		if (transferablePolicy == null)
			return false;
		
		return transferablePolicy.accept(tr);
	}
	

	@Override
	public String getDescription() {
		if (transferablePolicy == null)
			return null;
		
		return transferablePolicy.getDescription();
	}
	

	@Override
	public void handleTransferable(Transferable tr, boolean add) {
		if (transferablePolicy == null)
			return;
		
		transferablePolicy.handleTransferable(tr, add);
	}
	
}
