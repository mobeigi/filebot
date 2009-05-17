
package net.sourceforge.filebot.ui.panel.rename;


import static java.util.Collections.*;
import static net.sourceforge.tuned.ui.TunedUtilities.*;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.swing.AbstractAction;

import net.sourceforge.filebot.ResourceManager;


class RenameAction extends AbstractAction {
	
	private final RenameModel model;
	
	
	public RenameAction(RenameModel model) {
		this.model = model;
		
		putValue(NAME, "Rename");
		putValue(SMALL_ICON, ResourceManager.getIcon("action.rename"));
		putValue(SHORT_DESCRIPTION, "Rename files");
	}
	

	public void actionPerformed(ActionEvent evt) {
		List<Entry<File, File>> renameLog = new ArrayList<Entry<File, File>>();
		
		try {
			for (Entry<File, File> mapping : validate(model.getRenameMap(), getWindow(evt.getSource()))) {
				// rename file
				//DISABLE RENAME
				//				if (!mapping.getKey().renameTo(mapping.getValue()))
				//					throw new IOException(String.format("Failed to rename file: \"%s\".", mapping.getKey().getName()));
				
				// remember successfully renamed matches for possible revert
				renameLog.add(mapping);
			}
			
			// renamed all matches successfully
			if (renameLog.size() > 0) {
				Logger.getLogger("ui").info(String.format("%d files renamed.", renameLog.size()));
			}
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
		
		// remove renamed matches
		for (Entry<File, File> entry : renameLog) {
			// find index of source file
			int index = model.files().indexOf(entry.getKey());
			
			// remove complete match
			model.matches().remove(index);
		}
		
		// update history
		if (renameLog.size() > 0) {
			HistorySpooler.getInstance().append(renameLog);
		}
	}
	

	private Iterable<Entry<File, File>> validate(Map<File, File> renameMap, Window parent) {
		final List<Entry<File, File>> source = new ArrayList<Entry<File, File>>(renameMap.entrySet());
		
		List<String> destinationFileNameView = new AbstractList<String>() {
			
			@Override
			public String get(int index) {
				return source.get(index).getValue().getName();
			}
			

			@Override
			public String set(int index, String name) {
				Entry<File, File> entry = source.get(index);
				File old = entry.getValue();
				
				// update name
				entry.setValue(new File(old.getParent(), name));
				
				return old.getName();
			}
			

			@Override
			public int size() {
				return source.size();
			}
		};
		
		if (ValidateDialog.validate(parent, destinationFileNameView)) {
			// names have been validated via view
			return source;
		}
		
		// return empty list if validation was cancelled
		return emptyList();
	}
	
}
