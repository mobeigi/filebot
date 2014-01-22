package net.sourceforge.filebot.ui.analyze;

import static net.sourceforge.filebot.ui.NotificationLogging.*;
import static net.sourceforge.tuned.ExceptionUtilities.*;
import static net.sourceforge.tuned.FileUtilities.*;
import static net.sourceforge.tuned.ui.TunedUtilities.*;

import java.awt.Color;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import net.miginfocom.swing.MigLayout;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.archive.Archive;
import net.sourceforge.filebot.archive.FileMapper;
import net.sourceforge.filebot.ui.analyze.FileTree.FolderNode;
import net.sourceforge.filebot.vfs.FileInfo;
import net.sourceforge.tuned.FileUtilities;
import net.sourceforge.tuned.ui.GradientStyle;
import net.sourceforge.tuned.ui.LoadingOverlayPane;
import net.sourceforge.tuned.ui.ProgressDialog;
import net.sourceforge.tuned.ui.ProgressDialog.Cancellable;
import net.sourceforge.tuned.ui.SwingWorkerPropertyChangeAdapter;
import net.sourceforge.tuned.ui.notification.SeparatorBorder;

class ExtractTool extends Tool<TableModel> {

	private JTable table = new JTable(new ArchiveEntryModel());

	public ExtractTool() {
		super("Archives");

		table.setFillsViewportHeight(true);
		table.setAutoCreateRowSorter(true);
		table.setAutoCreateColumnsFromModel(true);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
		table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		table.setRowHeight(20);

		JScrollPane tableScrollPane = new JScrollPane(table);
		tableScrollPane.setBorder(new SeparatorBorder(2, new Color(0, 0, 0, 90), GradientStyle.TOP_TO_BOTTOM, SeparatorBorder.Position.BOTTOM));

		setLayout(new MigLayout("insets 0, nogrid, fill", "align center", "[fill][pref!]"));
		add(new LoadingOverlayPane(tableScrollPane, this, "20px", "30px"), "grow, wrap");
		add(new JButton(extractAction), "gap top rel, gap bottom unrel");
	}

	@Override
	protected void setModel(TableModel model) {
		table.setModel(model);
	}

	@Override
	protected TableModel createModelInBackground(FolderNode sourceModel) throws InterruptedException {
		List<ArchiveEntry> entries = new ArrayList<ArchiveEntry>();

		try {
			for (Iterator<File> iterator = sourceModel.fileIterator(); iterator.hasNext();) {
				File file = iterator.next();

				// ignore non-archives files and trailing multi-volume parts
				if (Archive.VOLUME_ONE_FILTER.accept(file)) {
					Archive archive = new Archive(file);
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
			UILogger.log(Level.WARNING, e.getMessage(), e);
		}

		return new ArchiveEntryModel(entries);
	}

	private Action extractAction = new AbstractAction("Extract All", ResourceManager.getIcon("package.extract")) {

		@Override
		public void actionPerformed(ActionEvent evt) {
			final List<File> archives = ((ArchiveEntryModel) table.getModel()).getArchiveList();
			if (archives.isEmpty()) {
				return;
			}

			Window window = getWindow(evt.getSource());
			JFileChooser chooser = new JFileChooser(archives.get(0).getParentFile());
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			chooser.setMultiSelectionEnabled(false);
			if (chooser.showSaveDialog(window) != JFileChooser.APPROVE_OPTION) {
				return;
			}

			final ExtractJob job = new ExtractJob(archives, chooser.getSelectedFile());

			final ProgressDialog dialog = new ProgressDialog(window, job);
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
		private final File outputRoot;

		public ExtractJob(Collection<File> archives, File outputRoot) {
			this.archives = archives.toArray(new File[archives.size()]);
			this.outputRoot = outputRoot;
		}

		@Override
		protected Void doInBackground() throws Exception {
			for (File it : archives) {
				try {
					// update progress dialog
					firePropertyChange("currentFile", null, it);

					Archive archive = new Archive(it);
					try {
						File outputFolder = (outputRoot != null) ? outputRoot : new File(it.getParentFile(), getNameWithoutExtension(it.getName()));
						FileMapper outputMapper = new FileMapper(outputFolder, false);
						archive.extract(outputMapper);
					} finally {
						archive.close();
					}
				} catch (Exception e) {
					UILogger.log(Level.WARNING, "Failed to extract archive: " + it.getName(), e);
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
