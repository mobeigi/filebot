
package net.sourceforge.filebot.ui.panel.rename;


import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.swing.AbstractAction;

import net.sourceforge.filebot.ResourceManager;


class RenameAction extends AbstractAction {
	
	private final RenameModel model;
	
	
	public RenameAction(RenameModel model) {
		super("Rename", ResourceManager.getIcon("action.rename"));
		
		putValue(SHORT_DESCRIPTION, "Rename files");
		
		this.model = model;
	}
	

	public void actionPerformed(ActionEvent evt) {
		List<Entry<File, File>> renameLog = new ArrayList<Entry<File, File>>();
		
		try {
			for (Entry<File, File> mapping : model.getRenameMap().entrySet()) {
				// rename file
				//DISABLE RENAME
				//				if (!mapping.getKey().renameTo(mapping.getValue()))
				//					throw new IOException(String.format("Failed to rename file: \"%s\".", mapping.getKey().getName()));
				
				// remember successfully renamed matches for possible revert
				renameLog.add(mapping);
			}
			
			// renamed all matches successfully
			Logger.getLogger("ui").info(String.format("%d files renamed.", renameLog.size()));
		} catch (Exception e) {
			// could not rename one of the files, revert all changes
			Logger.getLogger("ui").warning(e.getMessage());
			
			// revert rename operations in reverse order
			for (ListIterator<Entry<File, File>> iterator = renameLog.listIterator(renameLog.size()); iterator.hasPrevious();) {
				Entry<File, File> mapping = iterator.previous();
				
				if (mapping.getValue().renameTo(mapping.getKey())) {
					// remove reverted rename operation from log
					iterator.remove();
				} else {
					// failed to revert rename operation
					Logger.getLogger("ui").severe(String.format("Failed to revert file: \"%s\".", mapping.getValue().getName()));
				}
			}
		}
		
		// update history
		HistorySpooler.getInstance().append(renameLog);
	}
}
