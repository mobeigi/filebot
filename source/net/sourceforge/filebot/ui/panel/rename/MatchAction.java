
package net.sourceforge.filebot.ui.panel.rename;


import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.filebot.ui.panel.rename.entry.ListEntry;
import net.sourceforge.filebot.ui.panel.rename.match.Match;
import net.sourceforge.filebot.ui.panel.rename.match.Matcher;
import net.sourceforge.filebot.ui.panel.rename.similarity.LengthEqualsMetric;
import net.sourceforge.filebot.ui.panel.rename.similarity.MultiSimilarityMetric;
import net.sourceforge.filebot.ui.panel.rename.similarity.SimilarityMetric;
import net.sourceforge.filebot.ui.panel.rename.similarity.StringEqualsMetric;
import net.sourceforge.filebot.ui.panel.rename.similarity.StringSimilarityMetric;
import net.sourceforge.tuned.ui.ProgressDialog;
import net.sourceforge.tuned.ui.SwingWorkerProgressMonitor;


class MatchAction extends AbstractAction {
	
	private MultiSimilarityMetric metrics;
	
	private final RenameList namesList;
	private final RenameList filesList;
	
	private boolean matchName2File;
	
	public static final String MATCH_NAMES_2_FILES_DESCRIPTION = "Match names to files";
	public static final String MATCH_FILES_2_NAMES_DESCRIPTION = "Match files to names";
	
	
	public MatchAction(RenameList namesList, RenameList filesList) {
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
	

	public void actionPerformed(ActionEvent evt) {
		JComponent source = (JComponent) evt.getSource();
		
		SwingUtilities.getRoot(source).setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		
		RenameList primaryList = matchName2File ? namesList : filesList;
		RenameList secondaryList = matchName2File ? filesList : namesList;
		
		BackgroundMatcher backgroundMatcher = new BackgroundMatcher(primaryList, secondaryList, metrics);
		SwingWorkerProgressMonitor monitor = new SwingWorkerProgressMonitor(SwingUtilities.getWindowAncestor(source), backgroundMatcher);
		
		ProgressDialog progressDialog = monitor.getProgressDialog();
		progressDialog.setTitle("Matching ...");
		progressDialog.setHeader("Matching ...");
		progressDialog.setIcon((Icon) getValue(SMALL_ICON));
		
		backgroundMatcher.execute();
		
		try {
			// wait a for little while (matcher might finish within a few seconds)
			backgroundMatcher.get(monitor.getMillisToPopup(), TimeUnit.MILLISECONDS);
		} catch (TimeoutException ex) {
			// matcher will take longer, stop blocking EDT
			progressDialog.setVisible(true);
		} catch (Exception e) {
			Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, e.toString(), e);
		}
		
		SwingUtilities.getRoot(source).setCursor(Cursor.getDefaultCursor());
	}
	
	
	private class BackgroundMatcher extends SwingWorker<List<Match>, Object> {
		
		private final RenameList primaryList;
		private final RenameList secondaryList;
		
		private final Matcher matcher;
		
		
		public BackgroundMatcher(RenameList primaryList, RenameList secondaryList, SimilarityMetric similarityMetric) {
			this.primaryList = primaryList;
			this.secondaryList = secondaryList;
			
			matcher = new Matcher(primaryList.getEntries(), secondaryList.getEntries(), similarityMetric);
		}
		

		@Override
		protected List<Match> doInBackground() throws Exception {
			int total = matcher.remainingMatches();
			
			List<Match> matches = new ArrayList<Match>(total);
			
			while (matcher.hasNext() && !isCancelled()) {
				firePropertyChange(SwingWorkerProgressMonitor.PROPERTY_NOTE, null, getNote());
				
				matches.add(matcher.next());
				
				setProgress((matches.size() * 100) / total);
				firePropertyChange(SwingWorkerProgressMonitor.PROPERTY_PROGRESS_STRING, null, String.format("%d / %d", matches.size(), total));
			}
			
			return matches;
		}
		

		private String getNote() {
			ListEntry current = matcher.getFirstPrimaryEntry();
			
			if (current == null)
				current = matcher.getFirstSecondaryEntry();
			
			if (current == null)
				return "";
			
			return current.getName();
		}
		

		@Override
		protected void done() {
			if (isCancelled())
				return;
			
			try {
				List<Match> matches = get();
				
				primaryList.getModel().clear();
				secondaryList.getModel().clear();
				
				for (Match match : matches) {
					primaryList.getModel().add(match.getA());
					secondaryList.getModel().add(match.getB());
				}
				
				primaryList.getModel().addAll(matcher.getPrimaryList());
				secondaryList.getModel().addAll(matcher.getSecondaryList());
				
			} catch (Exception e) {
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, e.toString(), e);
			}
		}
		
	}
	
}
