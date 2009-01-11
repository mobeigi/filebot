
package net.sourceforge.filebot.ui.panel.rename;


import java.awt.Cursor;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.util.Arrays;
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
import net.sourceforge.filebot.similarity.LengthEqualsMetric;
import net.sourceforge.filebot.similarity.Match;
import net.sourceforge.filebot.similarity.Matcher;
import net.sourceforge.filebot.similarity.NameSimilarityMetric;
import net.sourceforge.filebot.similarity.SeasonEpisodeSimilarityMetric;
import net.sourceforge.filebot.similarity.SimilarityMetric;
import net.sourceforge.tuned.ui.ProgressDialog;
import net.sourceforge.tuned.ui.SwingWorkerPropertyChangeAdapter;
import net.sourceforge.tuned.ui.ProgressDialog.Cancellable;


class MatchAction extends AbstractAction {
	
	private final List<Object> namesModel;
	private final List<FileEntry> filesModel;
	
	private final SimilarityMetric[] metrics;
	
	
	public MatchAction(List<Object> namesModel, List<FileEntry> filesModel) {
		super("Match", ResourceManager.getIcon("action.match"));
		
		putValue(SHORT_DESCRIPTION, "Match names to files");
		
		this.namesModel = namesModel;
		this.filesModel = filesModel;
		
		metrics = new SimilarityMetric[3];
		
		// 1. pass: match by file length (fast, but only works when matching torrents or files)
		metrics[0] = new LengthEqualsMetric() {
			
			@Override
			protected long getLength(Object o) {
				if (o instanceof AbstractFileEntry) {
					return ((AbstractFileEntry) o).getLength();
				}
				
				return super.getLength(o);
			}
		};
		
		// 2. pass: match by season / episode numbers, or generic numeric similarity
		metrics[1] = new SeasonEpisodeSimilarityMetric();
		
		// 3. pass: match by generic name similarity (slow, but most matches will have been determined in second pass)
		metrics[2] = new NameSimilarityMetric();
	}
	

	public void actionPerformed(ActionEvent evt) {
		JComponent eventSource = (JComponent) evt.getSource();
		
		SwingUtilities.getRoot(eventSource).setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		
		BackgroundMatcher backgroundMatcher = new BackgroundMatcher(namesModel, filesModel, Arrays.asList(metrics));
		backgroundMatcher.execute();
		
		try {
			// wait a for little while (matcher might finish in less than a second)
			backgroundMatcher.get(2, TimeUnit.SECONDS);
		} catch (TimeoutException ex) {
			// matcher will probably take a while
			ProgressDialog progressDialog = createProgressDialog(SwingUtilities.getWindowAncestor(eventSource), backgroundMatcher);
			
			// display progress dialog and stop blocking EDT
			progressDialog.setVisible(true);
		} catch (Exception e) {
			Logger.getLogger("global").log(Level.SEVERE, e.toString(), e);
		}
		
		SwingUtilities.getRoot(eventSource).setCursor(Cursor.getDefaultCursor());
	}
	

	protected ProgressDialog createProgressDialog(Window parent, final BackgroundMatcher worker) {
		final ProgressDialog progressDialog = new ProgressDialog(parent, worker);
		
		// configure dialog
		progressDialog.setTitle("Matching...");
		progressDialog.setNote("Processing...");
		progressDialog.setIcon((Icon) getValue(SMALL_ICON));
		
		// close progress dialog when worker is finished
		worker.addPropertyChangeListener(new SwingWorkerPropertyChangeAdapter() {
			
			@Override
			protected void done(PropertyChangeEvent evt) {
				progressDialog.close();
			}
		});
		
		return progressDialog;
	}
	
	
	protected static class BackgroundMatcher extends SwingWorker<List<Match<Object, FileEntry>>, Void> implements Cancellable {
		
		private final List<Object> namesModel;
		private final List<FileEntry> filesModel;
		
		private final Matcher<Object, FileEntry> matcher;
		
		
		public BackgroundMatcher(List<Object> namesModel, List<FileEntry> filesModel, List<SimilarityMetric> metrics) {
			this.namesModel = namesModel;
			this.filesModel = filesModel;
			
			this.matcher = new Matcher<Object, FileEntry>(namesModel, filesModel, metrics);
		}
		

		@Override
		protected List<Match<Object, FileEntry>> doInBackground() throws Exception {
			return matcher.match();
		}
		

		@Override
		protected void done() {
			if (isCancelled())
				return;
			
			try {
				List<Match<Object, FileEntry>> matches = get();
				
				namesModel.clear();
				filesModel.clear();
				
				for (Match<Object, FileEntry> match : matches) {
					namesModel.add(match.getValue());
					filesModel.add(match.getCandidate());
				}
				
				namesModel.addAll(matcher.remainingValues());
				namesModel.addAll(matcher.remainingCandidates());
			} catch (Exception e) {
				Logger.getLogger("global").log(Level.SEVERE, e.toString(), e);
			}
		}
		

		@Override
		public boolean cancel() {
			return cancel(true);
		}
		
	}
	
}
