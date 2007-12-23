
package net.sourceforge.filebot.ui.panel.rename;


import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;

import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.filebot.ui.FileFormat;
import net.sourceforge.filebot.ui.MessageManager;
import net.sourceforge.filebot.ui.panel.rename.entry.FileEntry;
import net.sourceforge.filebot.ui.panel.rename.entry.ListEntry;


public class RenameAction extends AbstractAction {
	
	private NamesRenameList namesList;
	
	private FilesRenameList filesList;
	
	
	public RenameAction(NamesRenameList namesList, FilesRenameList filesList) {
		super("Rename", ResourceManager.getIcon("action.rename"));
		this.namesList = namesList;
		this.filesList = filesList;
		
		putValue(Action.SHORT_DESCRIPTION, "Rename files");
	}
	

	public void actionPerformed(ActionEvent e) {
		List<ListEntry<?>> nameEntries = namesList.getListEntries();
		List<FileEntry> fileEntries = filesList.getListEntries();
		
		int minLength = Math.min(nameEntries.size(), fileEntries.size());
		
		int i = 0;
		int errors = 0;
		
		for (i = 0; i < minLength; i++) {
			FileEntry fileEntry = fileEntries.get(i);
			File f = fileEntry.getValue();
			
			String newName = nameEntries.get(i).toString() + FileFormat.getSuffix(f, true);
			
			File newFile = new File(f.getParentFile(), newName);
			
			if (f.renameTo(newFile)) {
				filesList.getModel().removeElement(fileEntry);
			} else {
				errors++;
			}
		}
		
		if (errors > 0)
			MessageManager.showWarning((i - errors) + " of " + i + " files renamed.");
		else
			MessageManager.showInfo(i + " files renamed.");
		
		namesList.repaint();
		filesList.repaint();
	}
	
}
