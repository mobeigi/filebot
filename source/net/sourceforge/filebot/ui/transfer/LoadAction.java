
package net.sourceforge.filebot.ui.transfer;


import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;

import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.filebot.ui.transfer.TransferablePolicy.TransferAction;


public class LoadAction extends AbstractAction {
	
	private final TransferablePolicy transferablePolicy;
	
	
	public LoadAction(TransferablePolicy transferablePolicy) {
		super("Load", ResourceManager.getIcon("action.load"));
		
		this.transferablePolicy = transferablePolicy;
	}
	

	public void actionPerformed(ActionEvent e) {
		JFileChooser chooser = new JFileChooser();
		
		chooser.setFileFilter(new TransferablePolicyFileFilter(transferablePolicy));
		
		chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		chooser.setMultiSelectionEnabled(true);
		
		if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
			return;
		
		FileTransferable transferable = new FileTransferable(chooser.getSelectedFiles());
		
		TransferAction action = TransferAction.PUT;
		
		// if CTRL was pressed when the button was clicked, assume ADD action (same as with dnd)
		if ((e.getModifiers() & ActionEvent.CTRL_MASK) != 0)
			action = TransferAction.ADD;
		
		if (transferablePolicy.accept(transferable))
			transferablePolicy.handleTransferable(transferable, action);
	}
	
}
