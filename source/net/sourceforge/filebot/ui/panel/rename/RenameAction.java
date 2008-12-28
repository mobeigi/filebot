
package net.sourceforge.filebot.ui.panel.rename;


import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;

import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.ui.panel.rename.entry.FileEntry;
import net.sourceforge.filebot.ui.panel.rename.entry.ListEntry;
import net.sourceforge.tuned.FileUtil;


public class RenameAction extends AbstractAction {
	
	private final RenameList<ListEntry> namesList;
	private final RenameList<FileEntry> filesList;
	
	
	public RenameAction(RenameList<ListEntry> namesList, RenameList<FileEntry> filesList) {
		super("Rename", ResourceManager.getIcon("action.rename"));
		this.namesList = namesList;
		this.filesList = filesList;
		
		putValue(Action.SHORT_DESCRIPTION, "Rename files");
	}
	

	public void actionPerformed(ActionEvent e) {
		List<ListEntry> nameEntries = namesList.getEntries();
		List<FileEntry> fileEntries = filesList.getEntries();
		
		int minLength = Math.min(nameEntries.size(), fileEntries.size());
		
		int i = 0;
		int errors = 0;
		
		for (i = 0; i < minLength; i++) {
			FileEntry fileEntry = fileEntries.get(i);
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
			Logger.getLogger("ui").info(String.format("%d of %d files renamed.", i - errors, i));
		else
			Logger.getLogger("ui").info(String.format("%d files renamed.", i));
		
		namesList.repaint();
		filesList.repaint();
	}
}
