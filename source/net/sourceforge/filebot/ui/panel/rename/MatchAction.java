
package net.sourceforge.filebot.ui.panel.rename;


import java.awt.Cursor;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
import net.sourceforge.filebot.similarity.NumericSimilarityMetric;
import net.sourceforge.filebot.similarity.SeasonEpisodeSimilarityMetric;
import net.sourceforge.filebot.similarity.SimilarityMetric;
import net.sourceforge.filebot.similarity.SeasonEpisodeMatcher.SxE;
import net.sourceforge.filebot.web.Episode;
import net.sourceforge.tuned.FileUtilities;
import net.sourceforge.tuned.ui.ProgressDialog;
import net.sourceforge.tuned.ui.SwingWorkerPropertyChangeAdapter;
import net.sourceforge.tuned.ui.ProgressDialog.Cancellable;


class MatchAction extends AbstractAction {
	
	private final RenameModel<Object, File> model;
	
	private final Collection<SimilarityMetric> metrics;
	
	
	public MatchAction(RenameModel<Object, File> model) {
		super("Match", ResourceManager.getIcon("action.match"));
		
		this.model = model;
		this.metrics = createMetrics();
		
		putValue(SHORT_DESCRIPTION, "Match names to files");
	}
	

	protected Collection<SimilarityMetric> createMetrics() {
		SimilarityMetric[] metrics = new SimilarityMetric[4];
		
		// 1. pass: match by file length (fast, but only works when matching torrents or files)
		metrics[0] = new LengthEqualsMetric() {
			
			@Override
			protected long getLength(Object object) {
				if (object instanceof AbstractFileEntry) {
					return ((AbstractFileEntry) object).getLength();
				}
				
				return super.getLength(object);
			}
		};
		
		// 2. pass: match by season / episode numbers, or generic numeric similarity
		metrics[1] = new SeasonEpisodeSimilarityMetric() {
			
			@Override
			protected Collection<SxE> parse(Object o) {
				if (o instanceof Episode) {
					Episode episode = (Episode) o;
					
					try {
						// create SxE from episode
						return Collections.singleton(new SxE(episode.getSeasonNumber(), episode.getEpisodeNumber()));
					} catch (NumberFormatException e) {
						// some kind of special episode, no SxE
						return null;
					}
				}
				
				return super.parse(o);
			}
		};
		
		// 3. pass: match by generic name similarity (slow, but most matches will have been determined in second pass)
		metrics[2] = new NameSimilarityMetric() {
			
			@Override
			public float getSimilarity(Object o1, Object o2) {
				// normalize absolute similarity to similarity rank (20 ranks in total),
				// so we are less likely to fall for false positives
				return (float) (Math.floor(super.getSimilarity(o1, o2) * 20) / 20);
			}
			

			@Override
			protected String normalize(Object object) {
				if (object instanceof File) {
					// compare to filename without extension
					object = FileUtilities.getName((File) object);
				}
				
				return super.normalize(object);
			}
		};
		
		// 4. pass: match by generic numeric similarity
		metrics[3] = new NumericSimilarityMetric() {
			
			@Override
			protected String normalize(Object object) {
				if (object instanceof File) {
					// compare to filename without extension
					object = FileUtilities.getName((File) object);
				}
				
				return super.normalize(object);
			}
		};
		
		return Arrays.asList(metrics);
	}
	

	public Collection<SimilarityMetric> getMetrics() {
		return Collections.unmodifiableCollection(metrics);
	}
	

	public void actionPerformed(ActionEvent evt) {
		if (model.names().isEmpty() || model.files().isEmpty()) {
			return;
		}
		
		JComponent eventSource = (JComponent) evt.getSource();
		
		SwingUtilities.getRoot(eventSource).setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		
		BackgroundMatcher backgroundMatcher = new BackgroundMatcher(model, metrics);
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
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, e.toString(), e);
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
	
	
	protected class BackgroundMatcher extends SwingWorker<List<Match<Object, File>>, Void> implements Cancellable {
		
		private final Matcher<Object, File> matcher;
		
		
		public BackgroundMatcher(RenameModel<Object, File> model, Collection<SimilarityMetric> metrics) {
			// match names against files
			this.matcher = new Matcher<Object, File>(model.names(), model.files(), metrics);
		}
		

		@Override
		protected List<Match<Object, File>> doInBackground() throws Exception {
			return matcher.match();
		}
		

		@Override
		protected void done() {
			if (isCancelled())
				return;
			
			try {
				List<Match<Object, File>> matches = get();
				
				model.clear();
				
				// put new data into model
				for (Match<Object, File> match : matches) {
					model.names().add(match.getValue());
					model.files().add(match.getCandidate());
				}
				
				// insert objects that could not be matched at the end
				model.names().addAll(matcher.remainingValues());
				model.files().addAll(matcher.remainingCandidates());
			} catch (Exception e) {
				Logger.getLogger(getClass().getName()).log(Level.SEVERE, e.toString(), e);
			}
		}
		

		@Override
		public boolean cancel() {
			return cancel(true);
		}
		
	}
	
}
