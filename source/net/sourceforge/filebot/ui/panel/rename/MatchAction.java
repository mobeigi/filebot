
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

import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.ui.panel.rename.entry.FileEntry;
import net.sourceforge.filebot.ui.panel.rename.entry.ListEntry;
import net.sourceforge.filebot.ui.panel.rename.matcher.Match;
import net.sourceforge.filebot.ui.panel.rename.matcher.Matcher;
import net.sourceforge.filebot.ui.panel.rename.metric.CompositeSimilarityMetric;
import net.sourceforge.filebot.ui.panel.rename.metric.NumericSimilarityMetric;
import net.sourceforge.filebot.ui.panel.rename.metric.SimilarityMetric;
import net.sourceforge.tuned.ui.ProgressDialog;
import net.sourceforge.tuned.ui.SwingWorkerProgressMonitor;


class MatchAction extends AbstractAction {
	
	private CompositeSimilarityMetric metrics;
	
	private final RenameList<ListEntry> namesList;
	private final RenameList<FileEntry> filesList;
	
	private boolean matchName2File;
	
	public static final String MATCH_NAMES_2_FILES_DESCRIPTION = "Match names to files";
	public static final String MATCH_FILES_2_NAMES_DESCRIPTION = "Match files to names";
	
	
	public MatchAction(RenameList<ListEntry> namesList, RenameList<FileEntry> filesList) {
		super("Match");
		
		this.namesList = namesList;
		this.filesList = filesList;
		
		// length similarity will only effect torrent <-> file matches
		metrics = new CompositeSimilarityMetric(new NumericSimilarityMetric());
		
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
	

	public CompositeSimilarityMetric getMetrics() {
		return metrics;
	}
	

	public boolean isMatchName2File() {
		return matchName2File;
	}
	

	@SuppressWarnings("unchecked")
	public void actionPerformed(ActionEvent evt) {
		JComponent source = (JComponent) evt.getSource();
		
		SwingUtilities.getRoot(source).setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		
		RenameList<ListEntry> primaryList = (RenameList<ListEntry>) (matchName2File ? namesList : filesList);
		RenameList<ListEntry> secondaryList = (RenameList<ListEntry>) (matchName2File ? filesList : namesList);
		
		BackgroundMatcher backgroundMatcher = new BackgroundMatcher(primaryList, secondaryList, metrics);
		SwingWorkerProgressMonitor monitor = new SwingWorkerProgressMonitor(SwingUtilities.getWindowAncestor(source), backgroundMatcher);
		
		ProgressDialog progressDialog = monitor.getProgressDialog();
		progressDialog.setTitle("Matching ...");
		progressDialog.setHeader(progressDialog.getTitle());
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
	
	
	private static class BackgroundMatcher extends SwingWorker<List<Match>, Void> {
		
		private final RenameList<ListEntry> primaryList;
		private final RenameList<ListEntry> secondaryList;
		
		private final Matcher matcher;
		
		
		public BackgroundMatcher(RenameList<ListEntry> primaryList, RenameList<ListEntry> secondaryList, SimilarityMetric similarityMetric) {
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
