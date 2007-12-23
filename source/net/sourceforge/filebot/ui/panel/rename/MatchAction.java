
package net.sourceforge.filebot.ui.panel.rename;


import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.SwingUtilities;

import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.filebot.ui.panel.rename.entry.ListEntry;
import net.sourceforge.filebot.ui.panel.rename.match.Match;
import net.sourceforge.filebot.ui.panel.rename.match.Matcher;
import net.sourceforge.filebot.ui.panel.rename.similarity.LengthEqualsMetric;
import net.sourceforge.filebot.ui.panel.rename.similarity.MultiSimilarityMetric;
import net.sourceforge.filebot.ui.panel.rename.similarity.SimilarityMetric;
import net.sourceforge.filebot.ui.panel.rename.similarity.StringEqualsMetric;
import net.sourceforge.filebot.ui.panel.rename.similarity.StringSimilarityMetric;


public class MatchAction extends AbstractAction {
	
	private NamesRenameList namesList;
	private FilesRenameList filesList;
	private Matcher matcher;
	private SimilarityMetric metric;
	
	private boolean matchName2File;
	
	public static final String MATCH_NAMES_2_FILES_DESCRIPTION = "Match names to files";
	public static final String MATCH_FILES_2_NAMES_DESCRIPTION = "Match files to names";
	
	
	public MatchAction(NamesRenameList namesList, FilesRenameList filesList) {
		super("Match");
		
		this.namesList = namesList;
		this.filesList = filesList;
		
		MultiSimilarityMetric multiMetric = new MultiSimilarityMetric();
		multiMetric.addMetric(new StringSimilarityMetric());
		multiMetric.addMetric(new StringEqualsMetric());
		
		// length similarity will only effect torrent <-> file matches
		multiMetric.addMetric(new LengthEqualsMetric());
		
		matcher = new Matcher();
		metric = multiMetric;
		
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
	

	public SimilarityMetric getMetric() {
		return metric;
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
		
		List<Match> matches = matcher.match(listA, listB, metric);
		
		// insert matches into the UI
		DefaultListModel namesModel = namesList.getModel();
		DefaultListModel filesModel = filesList.getModel();
		
		namesModel.clear();
		filesModel.clear();
		
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
				namesModel.addElement(a);
			
			if (b != null)
				filesModel.addElement(b);
		}
		
		namesList.repaint();
		filesList.repaint();
		
		SwingUtilities.getRoot(namesList).setCursor(Cursor.getDefaultCursor());
	}
}
