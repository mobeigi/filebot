
package net.sourceforge.filebot.ui.transferablepolicies;


import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sourceforge.filebot.FileBotUtil;


public class MultiTransferablePolicy implements TransferablePolicy {
	
	private List<TransferablePolicy> policies = Collections.synchronizedList(new ArrayList<TransferablePolicy>());
	
	
	public MultiTransferablePolicy() {
		
	}
	

	public void addPolicy(TransferablePolicy policy) {
		policies.add(policy);
	}
	

	public void removePolicy(TransferablePolicy policy) {
		policies.remove(policy);
	}
	

	@Override
	public boolean accept(Transferable tr) {
		for (TransferablePolicy policy : policies) {
			if (policy.accept(tr))
				return true;
		}
		
		return false;
	}
	

	@Override
	public void handleTransferable(Transferable tr, boolean add) {
		for (TransferablePolicy policy : policies) {
			if (policy.accept(tr)) {
				policy.handleTransferable(tr, add);
				return;
			}
		}
	}
	

	@Override
	public String getDescription() {
		return getDescription(TransferablePolicy.class);
	}
	

	public String getDescription(Class<? extends TransferablePolicy> filter) {
		List<String> descriptions = new ArrayList<String>();
		
		for (TransferablePolicy policy : policies) {
			String desc = policy.getDescription();
			
			if (filter.isInstance(policy))
				descriptions.add(desc);
		}
		
		return FileBotUtil.join(descriptions, ", ");
	}
	
}
