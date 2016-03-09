package net.filebot.ui.rename;

import static net.filebot.Logging.*;
import static net.filebot.util.ui.SwingUI.*;

import java.awt.Cursor;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.SwingWorker;

import net.filebot.ResourceManager;
import net.filebot.similarity.EpisodeMetrics;
import net.filebot.similarity.Match;
import net.filebot.similarity.Matcher;
import net.filebot.similarity.SimilarityMetric;
import net.filebot.util.ui.ProgressDialog;
import net.filebot.util.ui.ProgressDialog.Cancellable;
import net.filebot.util.ui.SwingWorkerPropertyChangeAdapter;

class MatchAction extends AbstractAction {

	private final RenameModel model;

	public MatchAction(RenameModel model) {
		this.model = model;

		// initialize with default values
		setMatchMode(false);
	}

	public void setMatchMode(boolean strict) {
		putValue(NAME, "Match");
		putValue(SMALL_ICON, ResourceManager.getIcon(strict ? "action.match.strict" : "action.match"));
	}

	@Override
	public void actionPerformed(ActionEvent evt) {
		if (model.names().isEmpty() || model.files().isEmpty()) {
			return;
		}

		BackgroundMatcher backgroundMatcher = new BackgroundMatcher(model, EpisodeMetrics.defaultSequence(true));
		backgroundMatcher.execute();

		Window window = getWindow(evt.getSource());
		window.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		try {
			// wait a for little while (matcher might finish in less than a second)
			backgroundMatcher.get(2, TimeUnit.SECONDS);
		} catch (TimeoutException ex) {
			// matcher will probably take a while
			ProgressDialog dialog = createProgressDialog(window, backgroundMatcher);
			dialog.setLocation(getOffsetLocation(dialog.getOwner()));

			// display progress dialog and stop blocking EDT
			dialog.setVisible(true);
		} catch (Exception e) {
			debug.log(Level.SEVERE, e.getMessage(), e);
		} finally {
			window.setCursor(Cursor.getDefaultCursor());
		}
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

		public BackgroundMatcher(MatchModel<Object, File> model, SimilarityMetric[] metrics) {
			// match names against files
			this.matcher = new Matcher<Object, File>(model.values(), model.candidates(), false, metrics);
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
				model.addAll(matches);

				// insert objects that could not be matched at the end of the model
				model.addAll(matcher.remainingValues(), matcher.remainingCandidates());
			} catch (Exception e) {
				debug.log(Level.SEVERE, e.getMessage(), e);
			}
		}

		@Override
		public boolean cancel() {
			return cancel(true);
		}

	}

}
