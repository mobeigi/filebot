
package net.sourceforge.filebot.ui.panel.rename;


import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;

import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.filebot.ui.MessageManager;
import net.sourceforge.filebot.ui.panel.rename.entry.FileEntry;
import net.sourceforge.filebot.ui.panel.rename.entry.ListEntry;
import net.sourceforge.tuned.FileUtil;


public class RenameAction extends AbstractAction {
	
	private final RenameList namesList;
	private final RenameList filesList;
	
	
	public RenameAction(RenameList namesList, RenameList filesList) {
		super("Rename", ResourceManager.getIcon("action.rename"));
		this.namesList = namesList;
		this.filesList = filesList;
		
		putValue(Action.SHORT_DESCRIPTION, "Rename files");
	}
	

	public void actionPerformed(ActionEvent e) {
		List<ListEntry> nameEntries = namesList.getEntries();
		List<ListEntry> fileEntries = filesList.getEntries();
		
		int minLength = Math.min(nameEntries.size(), fileEntries.size());
		
		int i = 0;
		int errors = 0;
		
		for (i = 0; i < minLength; i++) {
			FileEntry fileEntry = (FileEntry) fileEntries.get(i);
			File f = fileEntry.getFile();
			
			String newName = nameEntries.get(i).toString() + FileUtil.getExtension(f, true);
			
			File newFile = new File(f.getParentFile(), newName);
			
			if (f.renameTo(newFile)) {
				filesList.getModel().remove(fileEntry);
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
