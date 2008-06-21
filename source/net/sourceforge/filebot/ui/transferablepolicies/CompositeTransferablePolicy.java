
package net.sourceforge.filebot.ui.transferablepolicies;


import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


public class CompositeTransferablePolicy implements TransferablePolicy {
	
	private List<TransferablePolicy> policies = Collections.synchronizedList(new ArrayList<TransferablePolicy>());
	
	
	public CompositeTransferablePolicy() {
		
	}
	

	public void addPolicy(TransferablePolicy policy) {
		policies.add(policy);
	}
	

	public void removePolicy(TransferablePolicy policy) {
		policies.remove(policy);
	}
	

	@Override
	public boolean accept(Transferable tr) {
		return getFirstAccepted(tr) != null;
	}
	

	@Override
	public void handleTransferable(Transferable tr, boolean add) {
		TransferablePolicy policy = getFirstAccepted(tr);
		
		if (policy == null)
			return;
		
		if (!add) {
			clear();
		}
		
		policy.handleTransferable(tr, add);
	}
	

	protected void clear() {
		
	}
	

	public TransferablePolicy getFirstAccepted(Transferable tr) {
		synchronized (policies) {
			for (TransferablePolicy policy : policies) {
				if (policy.accept(tr))
					return policy;
			}
		}
		
		return null;
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
		
		StringBuilder sb = new StringBuilder();
		Iterator<String> iterator = descriptions.iterator();
		
		while (iterator.hasNext()) {
			sb.append(iterator.next().toString());
			
			if (iterator.hasNext())
				sb.append(", ");
		}
		
		return sb.toString();
	}
	
}
