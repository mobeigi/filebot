
package net.sourceforge.filebot.ui.panel.rename;


import java.util.List;

import net.sourceforge.filebot.ui.panel.rename.entry.ListEntry;


public class NamesRenameList extends RenameList {
	
	public NamesRenameList() {
		setTitle("Names");
		setTransferablePolicy(new NamesListTransferablePolicy(this.getModel()));
	}
	

	@SuppressWarnings("unchecked")
	public List<ListEntry<?>> getListEntries() {
		return (List<ListEntry<?>>) getModel().getCopy();
	}
	
}
