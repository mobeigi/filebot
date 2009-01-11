
package net.sourceforge.filebot.ui.panel.rename;


import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.AbstractAction;

import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.similarity.Match;
import net.sourceforge.tuned.FileUtil;


public class RenameAction extends AbstractAction {
	
	private final List<Object> namesModel;
	private final List<FileEntry> filesModel;
	
	
	public RenameAction(List<Object> namesModel, List<FileEntry> filesModel) {
		super("Rename", ResourceManager.getIcon("action.rename"));
		
		putValue(SHORT_DESCRIPTION, "Rename files");
		
		this.namesModel = namesModel;
		this.filesModel = filesModel;
	}
	

	public void actionPerformed(ActionEvent evt) {
		
		Deque<Match<File, File>> renameMatches = new ArrayDeque<Match<File, File>>();
		Deque<Match<File, File>> revertMatches = new ArrayDeque<Match<File, File>>();
		
		Iterator<Object> names = namesModel.iterator();
		Iterator<FileEntry> files = filesModel.iterator();
		
		while (names.hasNext() && files.hasNext()) {
			File source = files.next().getFile();
			
			String targetName = names.next().toString() + FileUtil.getExtension(source, true);
			File target = new File(source.getParentFile(), targetName);
			
			renameMatches.addLast(new Match<File, File>(source, target));
		}
		
		try {
			int renameCount = renameMatches.size();
			
			for (Match<File, File> match : renameMatches) {
				// rename file
				if (!match.getValue().renameTo(match.getCandidate()))
					throw new IOException(String.format("Failed to rename file: %s.", match.getValue().getName()));
				
				// revert in reverse order if renaming of all matches fails
				revertMatches.addFirst(match);
			}
			
			// renamed all matches successfully
			Logger.getLogger("ui").info(String.format("%d files renamed.", renameCount));
		} catch (IOException e) {
			// rename failed
			Logger.getLogger("ui").warning(e.getMessage());
			
			boolean revertFailed = false;
			
			// revert rename operations
			for (Match<File, File> match : revertMatches) {
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
