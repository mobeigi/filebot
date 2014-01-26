package net.sourceforge.filebot.ui.rename;

import static java.util.Collections.*;
import static javax.swing.JOptionPane.*;
import static net.sourceforge.filebot.Settings.*;
import static net.sourceforge.filebot.ui.NotificationLogging.*;
import static net.sourceforge.tuned.ExceptionUtilities.*;
import static net.sourceforge.tuned.FileUtilities.*;
import static net.sourceforge.tuned.ui.TunedUtilities.*;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.util.AbstractList;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;

import net.sourceforge.filebot.Analytics;
import net.sourceforge.filebot.HistorySpooler;
import net.sourceforge.filebot.NativeRenameAction;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.StandardRenameAction;
import net.sourceforge.filebot.media.MediaDetection;
import net.sourceforge.filebot.similarity.Match;
import net.sourceforge.tuned.ui.ProgressDialog;
import net.sourceforge.tuned.ui.ProgressDialog.Cancellable;
import net.sourceforge.tuned.ui.SwingWorkerPropertyChangeAdapter;

class RenameAction extends AbstractAction {

	public static final String RENAME_ACTION = "RENAME_ACTION";

	private final RenameModel model;

	public RenameAction(RenameModel model) {
		this.model = model;
		resetValues();
	}

	public void resetValues() {
		putValue(RENAME_ACTION, StandardRenameAction.MOVE);
		putValue(NAME, "Rename");
		putValue(SMALL_ICON, ResourceManager.getIcon("action.rename"));
	}

	@Override
	public void actionPerformed(ActionEvent evt) {
		Window window = getWindow(evt.getSource());
		try {
			if (model.files().isEmpty() || model.values().isEmpty()) {
				UILogger.info("Nothing to rename. Please add some files and fetch naming data first.");
				return;
			}

			Map<File, File> renameMap = checkRenamePlan(validate(model.getRenameMap(), window), window);
			if (renameMap.isEmpty()) {
				return;
			}

			// start processing
			List<Match<Object, File>> matches = new ArrayList<Match<Object, File>>(model.matches());
			StandardRenameAction action = (StandardRenameAction) getValue(RENAME_ACTION);

			window.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

			if (useNativeShell() && isNativeActionSupported(action)) {
				RenameJob renameJob = new NativeRenameJob(renameMap, NativeRenameAction.valueOf(action.name()));
				renameJob.execute();
				try {
					renameJob.get(); // wait for native operation to finish or be cancelled
				} catch (CancellationException e) {
					// ignore
				}
			} else {
				RenameJob renameJob = new RenameJob(renameMap, action);
				renameJob.execute();

				try {
					// wait a for little while (renaming might finish in less than a second)
					renameJob.get(2, TimeUnit.SECONDS);
				} catch (TimeoutException ex) {
					// move/renaming will probably take a while
					ProgressDialog dialog = createProgressDialog(window, renameJob);
					dialog.setLocation(getOffsetLocation(dialog.getOwner()));
					dialog.setIndeterminate(true);

					// display progress dialog and stop blocking EDT
					window.setCursor(Cursor.getDefaultCursor());
					dialog.setVisible(true);
				}
			}

			// write metadata into xattr if xattr is enabled
			if (useExtendedFileAttributes() || useCreationDate()) {
				try {
					for (Match<Object, File> match : matches) {
						File file = match.getCandidate();
						Object meta = match.getValue();
						if (renameMap.containsKey(file) && meta != null) {
							File destination = resolveDestination(file, renameMap.get(file), false);
							if (destination.isFile()) {
								MediaDetection.storeMetaInfo(destination, meta, file.getName(), useExtendedFileAttributes(), useCreationDate());
							}
						}
					}
				} catch (Throwable e) {
					Logger.getLogger(RenameAction.class.getName()).warning("Failed to write xattr: " + e.getMessage());
				}
			}
		} catch (ExecutionException e) {
			// ignore, handled in rename worker
		} catch (Throwable e) {
			UILogger.log(Level.WARNING, e.getMessage(), e);
		}

		window.setCursor(Cursor.getDefaultCursor());
	}

	public boolean isNativeActionSupported(StandardRenameAction action) {
		try {
			return NativeRenameAction.isSupported() && NativeRenameAction.valueOf(action.name()) != null;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	private Map<File, File> checkRenamePlan(List<Entry<File, File>> renamePlan, Window parent) {
		// build rename map and perform some sanity checks
		Map<File, File> renameMap = new HashMap<File, File>();
		Set<File> destinationSet = new HashSet<File>();
		List<String> issues = new ArrayList<String>();

		for (Entry<File, File> mapping : renamePlan) {
			File source = mapping.getKey();
			File destination = mapping.getValue();

			// resolve destination
			if (!destination.isAbsolute()) {
				// same folder, different name
				destination = new File(source.getParentFile(), destination.getPath());
			}

			try {
				if (renameMap.containsKey(source))
					throw new IllegalArgumentException("Duplicate source file: " + source.getPath());

				if (destinationSet.contains(destination))
					throw new IllegalArgumentException("Conflict detected: " + mapping.getValue().getPath());

				if (destination.exists() && !resolveDestination(mapping.getKey(), mapping.getValue(), false).equals(mapping.getKey()))
					throw new IllegalArgumentException("File already exists: " + mapping.getValue().getPath());

				if (getExtension(destination) == null)
					throw new IllegalArgumentException("Missing extension: " + mapping.getValue().getPath());

				// use original mapping values
				renameMap.put(mapping.getKey(), mapping.getValue());
				destinationSet.add(destination);
			} catch (Exception e) {
				issues.add(e.getMessage());
			}
		}

		if (issues.size() > 0) {
			String text = "We found some issues. Do you to continue?";
			JList issuesComponent = new JList(issues.toArray()) {

				@Override
				public Dimension getPreferredScrollableViewportSize() {
					// adjust component size
					return new Dimension(80, 80);
				}
			};
			Object[] message = new Object[] { text, new JScrollPane(issuesComponent) };
			String[] actions = new String[] { "Continue", "Cancel" };
			JOptionPane pane = new JOptionPane(message, PLAIN_MESSAGE, YES_NO_OPTION, null, actions, actions[1]);

			// display option dialog
			pane.createDialog(getWindow(parent), "Conflicting Files").setVisible(true);

			if (pane.getValue() != actions[0]) {
				return emptyMap();
			}
		}

		return renameMap;
	}

	private List<Entry<File, File>> validate(Map<File, String> renameMap, Window parent) {
		final List<Entry<File, File>> source = new ArrayList<Entry<File, File>>(renameMap.size());

		for (Entry<File, String> entry : renameMap.entrySet()) {
			source.add(new SimpleEntry<File, File>(entry.getKey(), new File(entry.getValue())));
		}

		List<File> destinationFileNameView = new AbstractList<File>() {

			@Override
			public File get(int index) {
				return source.get(index).getValue();
			}

			@Override
			public File set(int index, File name) {
				return source.get(index).setValue(name);
			}

			@Override
			public int size() {
				return source.size();
			}
		};

		if (ValidateDialog.validate(parent, destinationFileNameView)) {
			// names have been validated via view
			return source;
		}

		// return empty list if validation was cancelled
		return emptyList();
	}

	protected ProgressDialog createProgressDialog(Window parent, final RenameJob job) {
		final ProgressDialog dialog = new ProgressDialog(parent, job);

		// configure dialog
		dialog.setTitle("Moving files...");
		dialog.setIcon((Icon) getValue(SMALL_ICON));

		// close progress dialog when worker is finished
		job.addPropertyChangeListener(new SwingWorkerPropertyChangeAdapter() {

			@Override
			protected void event(String name, Object oldValue, Object newValue) {
				if (name.equals("currentFile")) {
					int i = job.renameLog.size();
					int n = job.renameMap.size();
					dialog.setNote(String.format("%d of %d", i + 1, n));
					if (newValue instanceof File) {
						dialog.setWindowTitle("Moving " + ((File) oldValue).getName());
					}
				}
			}

			@Override
			protected void done(PropertyChangeEvent evt) {
				dialog.close();
			}
		});

		return dialog;
	}

	protected class RenameJob extends SwingWorker<Map<File, File>, Void> implements Cancellable {

		protected final net.sourceforge.filebot.RenameAction action;

		protected final Map<File, File> renameMap;
		protected final Map<File, File> renameLog;

		protected final Semaphore postprocess = new Semaphore(0);

		public RenameJob(Map<File, File> renameMap, net.sourceforge.filebot.RenameAction action) {
			this.action = action;
			this.renameMap = synchronizedMap(renameMap);
			this.renameLog = synchronizedMap(new LinkedHashMap<File, File>());
		}

		@Override
		protected Map<File, File> doInBackground() throws Exception {
			try {
				for (Entry<File, File> mapping : renameMap.entrySet()) {
					if (isCancelled())
						return renameLog;

					// update progress dialog
					firePropertyChange("currentFile", mapping.getKey(), mapping.getValue());

					// rename file, throw exception on failure
					File source = mapping.getKey();
					File destination = resolveDestination(mapping.getKey(), mapping.getValue(), false);
					if (!source.getAbsolutePath().equals(destination.getAbsolutePath())) {
						action.rename(source, destination);
					}

					// remember successfully renamed matches for history entry and possible revert
					renameLog.put(mapping.getKey(), mapping.getValue());
				}
			} finally {
				postprocess.release();
			}

			return renameLog;
		}

		@Override
		protected void done() {
			try {
				postprocess.acquire();
				this.get(); // grab exception if any
			} catch (Exception e) {
				if (!isCancelled()) {
					UILogger.log(Level.SEVERE, String.format("%s: %s", getRootCause(e).getClass().getSimpleName(), getRootCauseMessage(e)), e);
				} else {
					Logger.getLogger(RenameAction.class.getName()).log(Level.SEVERE, e.getMessage(), e);
				}
			}

			// collect renamed types
			final List<Class<?>> types = new ArrayList<Class<?>>();

			// remove renamed matches
			for (File source : renameLog.keySet()) {
				// find index of source file
				int index = model.files().indexOf(source);
				types.add(model.values().get(index).getClass());

				// remove complete match
				model.matches().remove(index);
			}

			if (renameLog.size() > 0) {
				UILogger.info(String.format("%d files renamed.", renameLog.size()));
				HistorySpooler.getInstance().append(renameLog.entrySet());

				// count global statistics
				for (Class<?> it : new HashSet<Class<?>>(types)) {
					Analytics.trackEvent("GUI", "Rename", it.getSimpleName(), frequency(types, it));
				}
			}
		}

		@Override
		public boolean cancel() {
			return cancel(true);
		}
	}

	protected class NativeRenameJob extends RenameJob implements Cancellable {

		public NativeRenameJob(Map<File, File> renameMap, NativeRenameAction action) {
			super(renameMap, action);
		}

		@Override
		protected Map<File, File> doInBackground() throws Exception {
			NativeRenameAction shell = (NativeRenameAction) action;

			// prepare delta, ignore files already named as desired
			Map<File, File> todo = new LinkedHashMap<File, File>();
			for (Entry<File, File> mapping : renameMap.entrySet()) {
				File source = mapping.getKey();
				File destination = resolveDestination(mapping.getKey(), mapping.getValue(), false);
				if (!source.equals(destination)) {
					todo.put(source, destination);
				}
			}

			// call native shell move/copy
			try {
				shell.rename(todo);
			} catch (CancellationException e) {
				// set as cancelled and propagate the exception
				super.cancel(false);
				throw e;
			} finally {
				// check status of renamed files
				for (Entry<File, File> it : renameMap.entrySet()) {
					if (resolveDestination(it.getKey(), it.getValue(), false).exists()) {
						renameLog.put(it.getKey(), it.getValue());
					}
				}
				postprocess.release();
			}

			return renameLog;
		}

		@Override
		public boolean cancel() {
			throw new UnsupportedOperationException();
		}
	}

}
