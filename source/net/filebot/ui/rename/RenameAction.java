package net.filebot.ui.rename;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static javax.swing.JOptionPane.*;
import static net.filebot.Logging.*;
import static net.filebot.Settings.*;
import static net.filebot.media.MediaDetection.*;
import static net.filebot.media.XattrMetaInfo.*;
import static net.filebot.util.ExceptionUtilities.*;
import static net.filebot.util.FileUtilities.*;
import static net.filebot.util.ui.SwingUI.*;

import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
import java.util.TreeSet;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;

import net.filebot.HistorySpooler;
import net.filebot.NativeRenameAction;
import net.filebot.ResourceManager;
import net.filebot.StandardRenameAction;
import net.filebot.mac.MacAppUtilities;
import net.filebot.similarity.Match;
import net.filebot.util.ui.ProgressDialog;
import net.filebot.util.ui.ProgressDialog.Cancellable;
import net.filebot.util.ui.SwingWorkerPropertyChangeAdapter;

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
		try {
			Window window = getWindow(evt.getSource());
			withWaitCursor(window, () -> {
				if (model.files().isEmpty() || model.values().isEmpty()) {
					log.info("Nothing to rename. Please add some files and fetch naming data first.");
					return;
				}

				Map<File, File> renameMap = checkRenamePlan(validate(model.getRenameMap(), window), window);
				if (renameMap.isEmpty()) {
					return;
				}

				// start processing
				List<Match<Object, File>> matches = new ArrayList<Match<Object, File>>(model.matches());
				StandardRenameAction action = (StandardRenameAction) getValue(RENAME_ACTION);

				if (useNativeShell() && isNativeActionSupported(action)) {
					RenameJob renameJob = new NativeRenameJob(renameMap, NativeRenameAction.valueOf(action.name()));
					renameJob.execute();

					// wait for native operation to finish or be cancelled
					try {
						renameJob.get();
					} catch (CancellationException e) {
						debug.finest(e::toString);
					}
				} else {
					RenameJob renameJob = new RenameJob(renameMap, action);
					renameJob.execute();

					// wait a little while (renaming might finish in less than a second)
					try {
						renameJob.get(2, TimeUnit.SECONDS);
					} catch (TimeoutException e) {
						// display progress dialog because move/rename might take a while
						ProgressDialog dialog = createProgressDialog(window, renameJob);
						dialog.setModalityType(ModalityType.APPLICATION_MODAL);
						dialog.setLocation(getOffsetLocation(dialog.getOwner()));
						dialog.setIndeterminate(true);
						dialog.setVisible(true);
					}
				}

				// store xattr
				storeMetaInfo(renameMap, matches);

				// delete empty folders
				if (action == StandardRenameAction.MOVE) {
					deleteEmptyFolders(renameMap);
				}
			});
		} catch (ExecutionException e) {
			// ignore, handled in rename worker
		} catch (Throwable e) {
			log.log(Level.WARNING, e.getMessage(), e);
		}
	}

	private void storeMetaInfo(Map<File, File> renameMap, List<Match<Object, File>> matches) {
		// write metadata into xattr if xattr is enabled
		for (Match<Object, File> match : matches) {
			File file = match.getCandidate();
			Object info = match.getValue();
			File destination = renameMap.get(file);
			if (info != null && destination != null) {
				destination = resolve(file, destination);
				if (destination.isFile()) {
					String original = file.getName();
					debug.finest(format("Store xattr: [%s, %s] => %s", info, original, destination));
					xattr.setMetaInfo(destination, info, original);
				}
			}
		}
	}

	private void deleteEmptyFolders(Map<File, File> renameMap) {
		// collect empty folders and files in reverse order
		Set<File> empty = new TreeSet<File>(reverseOrder());

		renameMap.forEach((s, d) -> {
			File sourceFolder = s.getParentFile();
			File destinationFolder = resolve(s, d).getParentFile();

			// destination folder is the source, or is inside the source folder
			if (destinationFolder.getPath().startsWith(sourceFolder.getPath())) {
				return;
			}

			// guess affected folder depth
			int relativePathSize = 0;
			try {
				relativePathSize = listStructurePathTail(s).size();
			} catch (Exception e) {
				debug.warning(e::toString);
			}

			for (int i = 0; i < relativePathSize && !isVolumeRoot(sourceFolder); sourceFolder = sourceFolder.getParentFile(), i++) {
				File[] children = sourceFolder.listFiles();
				if (children == null || !stream(children).allMatch(f -> empty.contains(f) || f.isHidden())) {
					return;
				}

				stream(children).forEach(empty::add);
				empty.add(sourceFolder);
			}
		});

		for (File f : empty) {
			try {
				debug.finest(format("Delete empty folder: %s", f));
				Files.delete(f.toPath());
			} catch (Exception e) {
				debug.warning(e::toString);
			}
		}
	}

	public boolean isNativeActionSupported(StandardRenameAction action) {
		try {
			return NativeRenameAction.isSupported() && NativeRenameAction.valueOf(action.name()) != null;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	private Map<File, File> checkRenamePlan(List<Entry<File, File>> renamePlan, Window parent) throws IOException {
		// ask for user permissions to output paths
		if (isMacSandbox()) {
			if (!MacAppUtilities.askUnlockFolders(parent, renamePlan.stream().flatMap(e -> Stream.of(e.getKey(), resolve(e.getKey(), e.getValue()))).map(f -> new File(f.getAbsolutePath())).collect(Collectors.toList()))) {
				return emptyMap();
			}
		}

		// build rename map and perform some sanity checks
		Map<File, File> renameMap = new HashMap<File, File>();
		Set<File> destinationSet = new HashSet<File>();
		List<String> issues = new ArrayList<String>();

		for (Entry<File, File> mapping : renamePlan) {
			File source = mapping.getKey();
			File destination = resolve(source, mapping.getValue());

			try {
				if (renameMap.containsKey(source))
					throw new IllegalArgumentException("Duplicate source file: " + source.getPath());

				if (destinationSet.contains(destination))
					throw new IllegalArgumentException("Conflict detected: " + mapping.getValue().getPath());

				if (destination.exists() && !resolve(mapping.getKey(), mapping.getValue()).equals(mapping.getKey()))
					throw new IllegalArgumentException("File already exists: " + mapping.getValue().getPath());

				if (getExtension(destination) == null && destination.isFile())
					throw new IllegalArgumentException("Missing extension: " + mapping.getValue().getPath());

				// use original mapping values
				renameMap.put(mapping.getKey(), mapping.getValue());
				destinationSet.add(destination);
			} catch (Exception e) {
				issues.add(e.getMessage());
			}
		}

		if (issues.size() > 0) {
			String text = "These files will be ignored. Do you want to continue?";
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
		dialog.setTitle("Processing files...");
		dialog.setIcon((Icon) getValue(SMALL_ICON));

		// close progress dialog when worker is finished
		job.addPropertyChangeListener(new SwingWorkerPropertyChangeAdapter() {

			@Override
			protected void event(String name, Object oldValue, Object newValue) {
				if (name.equals("currentFile")) {
					int i = job.renameLog.size();
					int n = job.renameMap.size();
					dialog.setNote(String.format("%d of %d", i + 1, n));

					// OSX LaF progress bar may not display progress bar text in indeterminate mode
					if (isMacApp()) {
						dialog.setTitle("Processing files... " + String.format("%d of %d", i + 1, n));
					}

					if (newValue instanceof File) {
						dialog.setWindowTitle("Processing " + ((File) oldValue).getName());
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

		protected final net.filebot.RenameAction action;

		protected final Map<File, File> renameMap;
		protected final Map<File, File> renameLog;

		protected final Semaphore postprocess = new Semaphore(0);

		public RenameJob(Map<File, File> renameMap, net.filebot.RenameAction action) {
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
					File destination = resolve(mapping.getKey(), mapping.getValue());
					boolean isSameFile = source.equals(destination);
					if (!isSameFile || (isSameFile && !source.getName().equals(destination.getName()))) {
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
					log.log(Level.SEVERE, String.format("%s: %s", getRootCause(e).getClass().getSimpleName(), getRootCauseMessage(e)), e);
				} else {
					debug.log(Level.SEVERE, e.getMessage(), e);
				}
			}

			// collect renamed types
			List<Class<?>> types = new ArrayList<Class<?>>();

			// remove renamed matches
			for (File source : renameLog.keySet()) {
				// find index of source file
				int index = model.files().indexOf(source);
				types.add(model.values().get(index).getClass());

				// remove complete match
				model.matches().remove(index);
			}

			if (renameLog.size() > 0) {
				log.info(String.format("%d files renamed.", renameLog.size()));
				HistorySpooler.getInstance().append(renameLog.entrySet());
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
				File destination = resolve(mapping.getKey(), mapping.getValue());
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
					if (resolve(it.getKey(), it.getValue()).exists()) {
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
