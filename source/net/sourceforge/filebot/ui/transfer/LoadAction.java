
package net.sourceforge.filebot.ui.transfer;


import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;

import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.filebot.ui.transferablepolicies.TransferablePolicy;
import net.sourceforge.filebot.ui.transferablepolicies.TransferablePolicySupport;


public class LoadAction extends AbstractAction {
	
	private TransferablePolicySupport transferablePolicySupport;
	
	
	public LoadAction(TransferablePolicySupport transferablePolicySupport) {
		super("Load", ResourceManager.getIcon("action.load"));
		this.transferablePolicySupport = transferablePolicySupport;
	}
	

	public void actionPerformed(ActionEvent e) {
		JFileChooser chooser = new JFileChooser();
		
		chooser.setFileFilter(new TransferablePolicyFileFilter(transferablePolicySupport.getTransferablePolicy()));
		
		chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		chooser.setMultiSelectionEnabled(true);
		
		if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
			return;
		
		FileTransferable transferable = new FileTransferable(chooser.getSelectedFiles());
		
		TransferablePolicy transferablePolicy = transferablePolicySupport.getTransferablePolicy();
		
		boolean add = ((e.getModifiers() & ActionEvent.CTRL_MASK) != 0);
		
		if (transferablePolicy.accept(transferable))
			transferablePolicy.handleTransferable(transferable, add);
	}
	
}
