
package net.sourceforge.filebot.ui.transferablepolicies;


import java.awt.datatransfer.Transferable;


public interface TransferablePolicy {
	
	public boolean accept(Transferable tr);
	

	public void handleTransferable(Transferable tr, boolean add);
	

	public String getDescription();
	
}
