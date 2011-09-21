
package net.sourceforge.filebot.ui.panel.rename;


import static java.util.Collections.*;
import static net.sourceforge.filebot.ui.NotificationLogging.*;
import static net.sourceforge.tuned.FileUtilities.*;
import static net.sourceforge.tuned.ui.TunedUtilities.*;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.AbstractAction;

import net.sourceforge.filebot.Analytics;
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
		List<Entry<File, String>> renameLog = new ArrayList<Entry<File, String>>();
		
		try {
			for (Entry<File, String> mapping : validate(model.getRenameMap(), getWindow(evt.getSource()))) {
				// rename file, throw exception on failure
				renameFile(mapping.getKey(), mapping.getValue());
				
				// remember successfully renamed matches for history entry and possible revert 
				renameLog.add(mapping);
			}
			
			// renamed all matches successfully
			if (renameLog.size() > 0) {
				UILogger.info(String.format("%d files renamed.", renameLog.size()));
			}
		} catch (Exception e) {
			// could not rename one of the files, revert all changes
			UILogger.warning(e.getMessage());
			
			// revert rename operations in reverse order
			for (ListIterator<Entry<File, String>> iterator = renameLog.listIterator(renameLog.size()); iterator.hasPrevious();) {
				Entry<File, String> mapping = iterator.previous();
				
				// revert rename
				File original = mapping.getKey();
				File current = new File(original.getParentFile(), mapping.getValue());
				
				if (current.renameTo(original)) {
					// remove reverted rename operation from log
					iterator.remove();
				} else {
					// failed to revert rename operation
					UILogger.severe("Failed to revert file: " + mapping.getValue());
				}
			}
		}
		
		// collect renamed types
		List<Class> types = new ArrayList<Class>();
		
		// remove renamed matches
		for (Entry<File, ?> entry : renameLog) {
			// find index of source file
			int index = model.files().indexOf(entry.getKey());
			types.add(model.values().get(index).getClass());
			
			// remove complete match
			model.matches().remove(index);
		}
		
		// update history
		if (renameLog.size() > 0) {
			HistorySpooler.getInstance().append(renameLog);
			
			for (Class it : new HashSet<Class>(types)) {
				Analytics.trackEvent("GUI", "Rename", it.getSimpleName(), frequency(types, it));
			}
		}
	}
	

	private Iterable<Entry<File, String>> validate(Map<File, String> renameMap, Window parent) {
		final List<Entry<File, String>> source = new ArrayList<Entry<File, String>>(renameMap.entrySet());
		
		List<String> destinationFileNameView = new AbstractList<String>() {
			
			@Override
			public String get(int index) {
				return source.get(index).getValue();
			}
			

			@Override
			public String set(int index, String name) {
				return source.get(index).setValue(name);
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
