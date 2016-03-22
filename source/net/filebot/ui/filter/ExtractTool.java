package net.filebot.ui.filter;

import static net.filebot.Logging.*;
import static net.filebot.UserFiles.*;
import static net.filebot.util.ExceptionUtilities.*;
import static net.filebot.util.FileUtilities.*;
import static net.filebot.util.ui.SwingUI.*;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import net.filebot.ResourceManager;
import net.filebot.archive.Archive;
import net.filebot.archive.FileMapper;
import net.filebot.cli.ConflictAction;
import net.filebot.util.FileUtilities;
import net.filebot.util.ui.GradientStyle;
import net.filebot.util.ui.LoadingOverlayPane;
import net.filebot.util.ui.ProgressDialog;
import net.filebot.util.ui.ProgressDialog.Cancellable;
import net.filebot.util.ui.SwingWorkerPropertyChangeAdapter;
import net.filebot.util.ui.notification.SeparatorBorder;
import net.filebot.vfs.FileInfo;
import net.filebot.vfs.SimpleFileInfo;
import net.miginfocom.swing.MigLayout;

class ExtractTool extends Tool<TableModel> {

	private JTable table = new JTable(new ArchiveEntryModel());

	public ExtractTool() {
		super("Archives");

		table.setAutoCreateRowSorter(true);
		table.setAutoCreateColumnsFromModel(true);
		table.setFillsViewportHeight(true);

		table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
		table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		table.setBackground(Color.white);
		table.setGridColor(new Color(0xEEEEEE));
		table.setRowHeight(25);

		JScrollPane tableScrollPane = new JScrollPane(table);
		tableScrollPane.setBorder(new SeparatorBorder(2, new Color(0, 0, 0, 90), GradientStyle.TOP_TO_BOTTOM, SeparatorBorder.Position.BOTTOM));

		setLayout(new MigLayout("insets 0, nogrid, fill", "align center", "[fill][pref!]"));
		add(new LoadingOverlayPane(tableScrollPane, this, "25px", "30px"), "grow, wrap");
		add(new JButton(extractAction), "gap top rel, gap bottom unrel");
	}

	@Override
	protected void setModel(TableModel model) {
		table.setModel(model);
	}

	@Override
	protected TableModel createModelInBackground(File root) throws InterruptedException {
		List<File> files = (root != null) ? FileUtilities.listFiles(root) : new ArrayList<File>();

		List<ArchiveEntry> entries = new ArrayList<ArchiveEntry>();
		try {
			for (File file : files) {
				// ignore non-archives files and trailing multi-volume parts
				if (Archive.VOLUME_ONE_FILTER.accept(file)) {
					Archive archive = Archive.open(file);
					try {
						for (FileInfo it : archive.listFiles()) {
							entries.add(new ArchiveEntry(file, it));
						}
					} finally {
						archive.close();
					}
				}

				// unwind thread, if we have been cancelled
				if (Thread.interrupted()) {
					throw new InterruptedException();
				}
			}
		} catch (Exception e) {
			// unwind thread, if we have been cancelled
			if (findCause(e, InterruptedException.class) != null) {
				throw findCause(e, InterruptedException.class);
			}
			log.log(Level.WARNING, e.getMessage(), e);
		}

		return new ArchiveEntryModel(entries);
	}

	private Action extractAction = new AbstractAction("Extract All", ResourceManager.getIcon("package.extract")) {

		@Override
		public void actionPerformed(ActionEvent evt) {
			final List<File> archives = ((ArchiveEntryModel) table.getModel()).getArchiveList();
			if (archives.isEmpty())
				return;

			File selectedFile = showOpenDialogSelectFolder(archives.get(0).getParentFile(), "Extract to ...", evt);
			if (selectedFile == null)
				return;

			final ExtractJob job = new ExtractJob(archives, selectedFile, null, true, ConflictAction.AUTO);
			final ProgressDialog dialog = new ProgressDialog(getWindow(evt.getSource()), job);
			dialog.setLocation(getOffsetLocation(dialog.getOwner()));
			dialog.setTitle("Extracting files...");
			dialog.setIcon((Icon) getValue(SMALL_ICON));
			dialog.setIndeterminate(true);

			// close progress dialog when worker is finished
			job.addPropertyChangeListener(new SwingWorkerPropertyChangeAdapter() {

				@Override
				protected void event(String name, Object oldValue, Object newValue) {
					if (name.equals("currentFile")) {
						String note = "Extracting " + ((File) newValue).getName();
						dialog.setNote(note);
						dialog.setWindowTitle(note);
					}
				}

				@Override
				protected void done(PropertyChangeEvent evt) {
					dialog.close();
				}
			});

			job.execute();
			dialog.setVisible(true);
		}
	};

	private static class ArchiveEntry {

		public final File archive;
		public final FileInfo entry;

		public ArchiveEntry(File archive, FileInfo entry) {
			this.archive = archive;
			this.entry = entry;
		}
	}

	private static class ArchiveEntryModel extends AbstractTableModel {

		private final ArchiveEntry[] data;

		public ArchiveEntryModel() {
			this.data = new ArchiveEntry[0];
		}

		public ArchiveEntryModel(Collection<ArchiveEntry> data) {
			this.data = data.toArray(new ArchiveEntry[data.size()]);
		}

		public List<File> getArchiveList() {
			Set<File> archives = new LinkedHashSet<File>();
			for (ArchiveEntry it : data) {
				archives.add(it.archive);
			}
			return new ArrayList<File>(archives);
		}

		@Override
		public int getRowCount() {
			return data.length;
		}

		@Override
		public int getColumnCount() {
			return 3;
		}

		@Override
		public String getColumnName(int column) {
			switch (column) {
			case 0:
				return "File";
			case 1:
				return "Path";
			case 2:
				return "Size";
			}
			return null;
		}

		@Override
		public Object getValueAt(int row, int column) {
			switch (column) {
			case 0:
				return data[row].entry.getName();
			case 1:
				File root = new File(data[row].archive.getName());
				File prefix = data[row].entry.toFile().getParentFile();
				File path = (prefix == null) ? root : new File(root, prefix.getPath());
				return normalizePathSeparators(path.getPath());
			case 2:
				return FileUtilities.formatSize(data[row].entry.getLength());
			}

			return null;
		}

	}

	protected static class ExtractJob extends SwingWorker<Void, Void> implements Cancellable {

		private final File[] archives;
		private final File outputFolder;

		private final FileFilter filter;
		private final boolean forceExtractAll;
		private final ConflictAction conflictAction;

		public ExtractJob(Collection<File> archives, File outputFolder, FileFilter filter, boolean forceExtractAll, ConflictAction conflictAction) {
			this.archives = archives.toArray(new File[archives.size()]);
			this.outputFolder = outputFolder;
			this.filter = filter;
			this.forceExtractAll = forceExtractAll;
			this.conflictAction = conflictAction;
		}

		@Override
		protected Void doInBackground() throws Exception {
			for (File file : archives) {
				try {
					// update progress dialog
					firePropertyChange("currentFile", null, file);

					Archive archive = Archive.open(file);
					try {
						final FileMapper outputMapper = new FileMapper(outputFolder);

						final List<FileInfo> outputMapping = new ArrayList<FileInfo>();
						for (FileInfo it : archive.listFiles()) {
							File outputPath = outputMapper.getOutputFile(it.toFile());
							outputMapping.add(new SimpleFileInfo(outputPath.getPath(), it.getLength()));
						}

						final Set<FileInfo> selection = new TreeSet<FileInfo>();
						for (FileInfo future : outputMapping) {
							if (filter == null || filter.accept(future.toFile())) {
								selection.add(future);
							}
						}

						// check if there is anything to extract at all
						if (selection.isEmpty()) {
							continue;
						}

						boolean skip = true;
						for (FileInfo future : filter == null || forceExtractAll ? outputMapping : selection) {
							if (conflictAction == ConflictAction.AUTO) {
								skip &= (future.toFile().exists() && future.getLength() == future.toFile().length());
							} else {
								skip &= (future.toFile().exists());
							}
						}

						if (!skip || conflictAction == ConflictAction.OVERRIDE) {
							if (filter == null || forceExtractAll) {
								// extract all files
								archive.extract(outputMapper.getOutputDir());
							} else {
								// extract files selected by the given filter
								archive.extract(outputMapper.getOutputDir(), new FileFilter() {

									@Override
									public boolean accept(File entry) {
										return selection.contains(outputMapper.getOutputFile(entry));
									}
								});
							}
						}
					} finally {
						archive.close();
					}
				} catch (Exception e) {
					log.log(Level.WARNING, "Failed to extract archive: " + file.getName(), e);
				}

				if (isCancelled()) {
					throw new CancellationException();
				}
			}
			return null;
		}

		@Override
		public boolean cancel() {
			return cancel(true);
		}

	}

}
