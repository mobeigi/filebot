package net.filebot.ui.rename;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static java.util.stream.Collectors.*;
import static javax.swing.JOptionPane.*;
import static net.filebot.Logging.*;
import static net.filebot.Settings.*;
import static net.filebot.media.MediaDetection.*;
import static net.filebot.media.XattrMetaInfo.*;
import static net.filebot.util.ExceptionUtilities.*;
import static net.filebot.util.FileUtilities.*;
import static net.filebot.util.ui.SwingUI.*;

import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
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
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Stream;

import javax.swing.AbstractAction;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

import net.filebot.HistorySpooler;
import net.filebot.NativeRenameAction;
import net.filebot.ResourceManager;
import net.filebot.StandardRenameAction;
import net.filebot.mac.MacAppUtilities;
import net.filebot.similarity.Match;
import net.filebot.util.ui.ProgressMonitor;
import net.filebot.util.ui.ProgressMonitor.ProgressWorker;

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
		if (model.names().isEmpty() || model.files().isEmpty()) {
			log.info("Nothing to rename. New Names is empty. Please <Fetch Data> first.");
			return;
		}

		try {
			Window window = getWindow(evt.getSource());
			withWaitCursor(window, () -> {
				Map<File, File> renameMap = checkRenamePlan(validate(model.getRenameMap(), window), window);
				if (renameMap.isEmpty()) {
					return;
				}

				List<Match<Object, File>> matches = new ArrayList<Match<Object, File>>(model.matches());
				StandardRenameAction action = (StandardRenameAction) getValue(RENAME_ACTION);

				// start processing
				Map<File, File> renameLog = new LinkedHashMap<File, File>();

				try {
					if (useNativeShell() && isNativeActionSupported(action)) {
						// call on EDT
						RenameWorker worker = new NativeRenameWorker(renameMap, renameLog, NativeRenameAction.valueOf(action.name()));
						worker.call(null, null, null);
					} else {
						// call and wait
						RenameWorker worker = new RenameWorker(renameMap, renameLog, action);
						String message = String.format("%sing %d %s. This may take a while.", action.getDisplayName(), renameMap.size(), renameMap.size() == 1 ? "file" : "files");
						ProgressMonitor.runTask(action.getDisplayName(), message, worker).get();
					}
				} catch (CancellationException e) {
					debug.finest(e::toString);
				} catch (Exception e) {
					log.log(Level.SEVERE, String.format("%s: %s", getRootCause(e).getClass().getSimpleName(), getRootCauseMessage(e)), e);
				}

				// abort if nothing happened
				if (renameLog.isEmpty()) {
					return;
				}

				log.info(String.format("%d files renamed.", renameLog.size()));

				// remove renamed matches
				renameLog.forEach((from, to) -> {
					model.matches().remove(model.files().indexOf(from));
				});

				HistorySpooler.getInstance().append(renameLog.entrySet());

				// store xattr
				storeMetaInfo(renameMap, matches);

				// delete empty folders
				if (action == StandardRenameAction.MOVE) {
					deleteEmptyFolders(renameLog);
				}
			});
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
		Set<File> deleteFiles = new TreeSet<File>();

		renameMap.forEach((s, d) -> {
			File sourceFolder = s.getParentFile();
			File destinationFolder = resolve(s, d).getParentFile();

			// destination folder is the source, or is inside the source folder
			if (d.getParentFile() == null || destinationFolder.getPath().startsWith(sourceFolder.getPath())) {
				return;
			}

			try {
				// guess affected folder depth
				int tailSize = listStructurePathTail(d.getParentFile()).size();

				for (int i = 0; i < tailSize && !isStructureRoot(sourceFolder); sourceFolder = sourceFolder.getParentFile(), i++) {
					File[] children = sourceFolder.listFiles();
					if (children == null || !stream(children).allMatch(f -> deleteFiles.contains(f) || isThumbnailStore(f))) {
						return;
					}

					stream(children).forEach(deleteFiles::add);
					deleteFiles.add(sourceFolder);
				}
			} catch (Exception e) {
				debug.warning(e::toString);
			}
		});

		// use system trash to delete left-behind empty folders / hidden files
		try {
			for (File file : deleteFiles) {
				if (file.exists()) {
					NativeRenameAction.trash(file);
				}
			}
		} catch (Throwable e) {
			debug.log(Level.WARNING, e, e::getMessage);
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
			if (!MacAppUtilities.askUnlockFolders(parent, renamePlan.stream().flatMap(e -> Stream.of(e.getKey(), resolve(e.getKey(), e.getValue()))).map(f -> new File(f.getAbsolutePath())).collect(toList()))) {
				return emptyMap();
			}
		}

		// build rename map and perform some sanity checks
		Map<File, File> renameMap = new HashMap<File, File>();
		Set<File> destinationFiles = new HashSet<File>();
		List<String> issues = new ArrayList<String>();

		for (Entry<File, File> mapping : renamePlan) {
			File source = mapping.getKey();
			File destination = resolve(source, mapping.getValue());

			try {
				if (renameMap.containsKey(source))
					throw new IllegalArgumentException("Duplicate input path: " + source.getPath());

				if (destinationFiles.contains(destination))
					throw new IllegalArgumentException("Duplicate output path: " + mapping.getValue());

				if (destination.exists() && !resolve(mapping.getKey(), mapping.getValue()).equals(mapping.getKey()))
					throw new IllegalArgumentException("File already exists: " + mapping.getValue().getPath());

				if (getExtension(destination) == null && destination.isFile())
					throw new IllegalArgumentException("Missing extension: " + mapping.getValue().getPath());

				// use original mapping values
				renameMap.put(mapping.getKey(), mapping.getValue());
				destinationFiles.add(destination);
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

	protected static class RenameWorker implements ProgressWorker<Map<File, File>> {

		protected final Map<File, File> renameMap;
		protected final Map<File, File> renameLog;

		protected final net.filebot.RenameAction action;

		protected boolean cancelled = false;

		public RenameWorker(Map<File, File> renameMap, Map<File, File> renameLog, net.filebot.RenameAction action) {
			this.renameMap = renameMap;
			this.renameLog = renameLog;
			this.action = action;
		}

		@Override
		public Map<File, File> call(Consumer<String> message, BiConsumer<Long, Long> progress, Supplier<Boolean> cancelled) throws Exception {
			for (Entry<File, File> mapping : renameMap.entrySet()) {
				if (cancelled.get()) {
					return renameLog;
				}

				message.accept(mapping.getKey().getName());

				// rename file, throw exception on failure
				File source = mapping.getKey();
				File destination = resolve(mapping.getKey(), mapping.getValue());

				if (!equalsCaseSensitive(source, destination)) {
					action.rename(source, destination);
				}

				// remember successfully renamed matches for history entry and possible revert
				renameLog.put(mapping.getKey(), mapping.getValue());
			}

			return renameLog;
		}
	}

	protected static class NativeRenameWorker extends RenameWorker {

		public NativeRenameWorker(Map<File, File> renameMap, Map<File, File> renameLog, NativeRenameAction action) {
			super(renameMap, renameLog, action);
		}

		@Override
		public Map<File, File> call(Consumer<String> message, BiConsumer<Long, Long> progress, Supplier<Boolean> cancelled) throws Exception {
			NativeRenameAction shell = (NativeRenameAction) action;

			// prepare delta, ignore files already named as desired
			Map<File, File> renamePlan = new LinkedHashMap<File, File>();
			for (Entry<File, File> mapping : renameMap.entrySet()) {
				File source = mapping.getKey();
				File destination = resolve(mapping.getKey(), mapping.getValue());
				if (!source.equals(destination)) {
					renamePlan.put(source, destination);
				}
			}

			// call native shell move/copy
			try {
				shell.rename(renamePlan);
			} catch (CancellationException e) {
				debug.finest(e::getMessage);
			}

			for (Entry<File, File> it : renameMap.entrySet()) {
				if (resolve(it.getKey(), it.getValue()).exists()) {
					renameLog.put(it.getKey(), it.getValue());
				}
			}

			return renameLog;
		}

	}

}
