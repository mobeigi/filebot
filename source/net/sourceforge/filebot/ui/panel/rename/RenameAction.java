
package net.sourceforge.filebot.ui.panel.rename;


import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Logger;

import javax.swing.AbstractAction;

import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.similarity.Match;
import net.sourceforge.tuned.FileUtil;


class RenameAction extends AbstractAction {
	
	private final RenameModel model;
	
	
	public RenameAction(RenameModel model) {
		super("Rename", ResourceManager.getIcon("action.rename"));
		
		putValue(SHORT_DESCRIPTION, "Rename files");
		
		this.model = model;
	}
	

	public void actionPerformed(ActionEvent evt) {
		
		Deque<Match<File, File>> todoQueue = new ArrayDeque<Match<File, File>>();
		Deque<Match<File, File>> doneQueue = new ArrayDeque<Match<File, File>>();
		
		for (Match<Object, FileEntry> match : model.matches()) {
			File source = match.getCandidate().getFile();
			
			String newName = match.getValue().toString() + FileUtil.getExtension(source, true);
			File target = new File(source.getParentFile(), newName);
			
			todoQueue.addLast(new Match<File, File>(source, target));
		}
		
		try {
			int renameCount = todoQueue.size();
			
			for (Match<File, File> match : todoQueue) {
				// rename file
				if (!match.getValue().renameTo(match.getCandidate()))
					throw new IOException(String.format("Failed to rename file: %s.", match.getValue().getName()));
				
				// revert in reverse order if renaming of all matches fails
				doneQueue.addFirst(match);
			}
			
			// renamed all matches successfully
			Logger.getLogger("ui").info(String.format("%d files renamed.", renameCount));
		} catch (IOException e) {
			// rename failed
			Logger.getLogger("ui").warning(e.getMessage());
			
			boolean revertFailed = false;
			
			// revert rename operations
			for (Match<File, File> match : doneQueue) {
				if (!match.getCandidate().renameTo(match.getValue())) {
					revertFailed = true;
				}
			}
			
			if (revertFailed) {
				Logger.getLogger("ui").severe("Failed to revert all rename operations.");
			}
		}
		
	}
}
