
package net.sourceforge.filebot.ui.transfer;


import net.sourceforge.filebot.ui.transferablepolicies.TransferablePolicy;


public interface TransferablePolicySupport {
	
	public void setTransferablePolicy(TransferablePolicy transferablePolicy);
	

	public TransferablePolicy getTransferablePolicy();
}
