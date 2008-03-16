
package net.sourceforge.filebot.ui.panel.rename;


import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.SwingUtilities;

import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.filebot.ui.panel.rename.entry.ListEntry;
import net.sourceforge.filebot.ui.panel.rename.match.Match;
import net.sourceforge.filebot.ui.panel.rename.match.Matcher;
import net.sourceforge.filebot.ui.panel.rename.similarity.LengthEqualsMetric;
import net.sourceforge.filebot.ui.panel.rename.similarity.MultiSimilarityMetric;
import net.sourceforge.filebot.ui.panel.rename.similarity.StringEqualsMetric;
import net.sourceforge.filebot.ui.panel.rename.similarity.StringSimilarityMetric;


class MatchAction extends AbstractAction {
	
	private NamesRenameList namesList;
	private FilesRenameList filesList;
	
	private MultiSimilarityMetric metrics;
	
	private Matcher matcher = new Matcher();
	
	private boolean matchName2File;
	
	public static final String MATCH_NAMES_2_FILES_DESCRIPTION = "Match names to files";
	public static final String MATCH_FILES_2_NAMES_DESCRIPTION = "Match files to names";
	
	
	public MatchAction(NamesRenameList namesList, FilesRenameList filesList) {
		super("Match");
		
		this.namesList = namesList;
		this.filesList = filesList;
		
		// length similarity will only effect torrent <-> file matches
		metrics = new MultiSimilarityMetric(new StringSimilarityMetric(), new StringEqualsMetric(), new LengthEqualsMetric());
		
		setMatchName2File(true);
	}
	

	public void setMatchName2File(boolean matchName2File) {
		this.matchName2File = matchName2File;
		
		if (matchName2File) {
			putValue(SMALL_ICON, ResourceManager.getIcon("action.match.name2file"));
			putValue(SHORT_DESCRIPTION, MATCH_NAMES_2_FILES_DESCRIPTION);
		} else {
			putValue(SMALL_ICON, ResourceManager.getIcon("action.match.file2name"));
			putValue(SHORT_DESCRIPTION, MATCH_FILES_2_NAMES_DESCRIPTION);
		}
	}
	

	public MultiSimilarityMetric getMetrics() {
		return metrics;
	}
	

	public boolean isMatchName2File() {
		return matchName2File;
	}
	

	public void actionPerformed(ActionEvent e) {
		SwingUtilities.getRoot(namesList).setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		
		List<? extends ListEntry<?>> listA, listB;
		
		if (matchName2File) {
			listA = namesList.getListEntries();
			listB = filesList.getListEntries();
		} else {
			listB = namesList.getListEntries();
			listA = filesList.getListEntries();
		}
		
		List<Match> matches = matcher.match(listA, listB, metrics);
		
		List<ListEntry<?>> names = new ArrayList<ListEntry<?>>();
		List<ListEntry<?>> files = new ArrayList<ListEntry<?>>();
		
		for (Match match : matches) {
			ListEntry<?> a, b;
			
			if (matchName2File) {
				a = match.getA();
				b = match.getB();
			} else {
				b = match.getA();
				a = match.getB();
			}
			
			if (a != null)
				names.add(a);
			
			if (b != null)
				files.add(b);
		}
		
		namesList.getModel().set(names);
		filesList.getModel().set(files);
		
		SwingUtilities.getRoot(namesList).setCursor(Cursor.getDefaultCursor());
	}
}
