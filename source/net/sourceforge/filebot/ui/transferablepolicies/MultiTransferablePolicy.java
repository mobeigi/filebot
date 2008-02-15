
package net.sourceforge.filebot.ui.transferablepolicies;


import java.awt.datatransfer.Transferable;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Iterator;


public class MultiTransferablePolicy extends TransferablePolicy {
	
	private ArrayList<TransferablePolicy> policies = new ArrayList<TransferablePolicy>();
	
	
	public MultiTransferablePolicy() {
		
	}
	

	public void addPolicy(TransferablePolicy policy) {
		policies.add(policy);
		policy.addPropertyChangeListener(relayListener);
	}
	

	public void removePolicy(TransferablePolicy policy) {
		policy.removePropertyChangeListener(relayListener);
		policies.remove(policy);
	}
	

	@Override
	public boolean accept(Transferable tr) {
		if (!isEnabled())
			return false;
		
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
	
	private final PropertyChangeListener relayListener = new PropertyChangeListener() {
		
		public void propertyChange(PropertyChangeEvent evt) {
			firePropertyChange(evt);
		}
	};
	
	
	@Override
	public String getDescription() {
		return getDescription(TransferablePolicy.class);
	}
	

	public String getDescription(Class<? extends TransferablePolicy> filter) {
		StringBuffer sb = new StringBuffer();
		
		ArrayList<String> descriptions = new ArrayList<String>();
		
		for (TransferablePolicy policy : policies) {
			String desc = policy.getDescription();
			
			if (filter.isInstance(policy))
				descriptions.add(desc);
		}
		
		Iterator<String> it = descriptions.iterator();
		
		while (it.hasNext()) {
			String desc = it.next();
			sb.append(desc);
			
			if (it.hasNext())
				sb.append(", ");
		}
		
		return sb.toString();
	}
	
}
