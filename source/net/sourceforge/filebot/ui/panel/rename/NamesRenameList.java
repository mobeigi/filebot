
package net.sourceforge.filebot.ui.panel.rename;


import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;

import net.sourceforge.filebot.ui.panel.rename.entry.ListEntry;


public class NamesRenameList extends RenameList {
	
	public NamesRenameList() {
		setTitle("Names");
		setTransferablePolicy(new NamesRenameListTransferablePolicy(this.getModel()));
	}
	

	public List<ListEntry<?>> getListEntries() {
		DefaultListModel model = getModel();
		
		List<ListEntry<?>> entries = new ArrayList<ListEntry<?>>();
		
		for (int i = 0; i < model.getSize(); i++)
			entries.add((ListEntry<?>) model.get(i));
		
		return entries;
	}
	
}
