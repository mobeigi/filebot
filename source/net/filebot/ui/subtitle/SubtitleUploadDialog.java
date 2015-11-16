package net.filebot.ui.subtitle;

import static net.filebot.MediaTypes.*;
import static net.filebot.UserFiles.*;
import static net.filebot.media.MediaDetection.*;
import static net.filebot.util.ui.SwingUI.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.EventObject;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.SwingWorker;
import javax.swing.event.CellEditorListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import net.filebot.Language;
import net.filebot.ResourceManager;
import net.filebot.WebServices;
import net.filebot.media.MediaDetection;
import net.filebot.similarity.SeriesNameMatcher;
import net.filebot.ui.LanguageComboBox;
import net.filebot.ui.SelectDialog;
import net.filebot.util.FileUtilities;
import net.filebot.util.ui.AbstractBean;
import net.filebot.util.ui.EmptySelectionModel;
import net.filebot.web.Movie;
import net.filebot.web.OpenSubtitlesClient;
import net.filebot.web.SearchResult;
import net.filebot.web.SubtitleSearchResult;
import net.filebot.web.TheTVDBSeriesInfo;
import net.filebot.web.VideoHashSubtitleService.CheckResult;
import net.miginfocom.swing.MigLayout;

public class SubtitleUploadDialog extends JDialog {

	private final JTable subtitleMappingTable = createTable();

	private final OpenSubtitlesClient database;

	private ExecutorService checkExecutorService = Executors.newSingleThreadExecutor();
	private ExecutorService uploadExecutorService;

	public SubtitleUploadDialog(OpenSubtitlesClient database, Window owner) {
		super(owner, "Upload Subtitles", ModalityType.DOCUMENT_MODAL);

		this.database = database;

		JComponent content = (JComponent) getContentPane();
		content.setLayout(new MigLayout("fill, insets dialog, nogrid", "", "[fill][pref!]"));

		content.add(new JScrollPane(subtitleMappingTable), "grow, wrap");

		content.add(new JButton(uploadAction), "tag ok");
		content.add(new JButton(finishAction), "tag cancel");
	}

	protected JTable createTable() {
		JTable table = new JTable(new SubtitleMappingTableModel());
		table.setDefaultRenderer(Movie.class, new MovieRenderer());
		table.setDefaultRenderer(File.class, new FileRenderer());
		table.setDefaultRenderer(Language.class, new LanguageRenderer());
		table.setDefaultRenderer(SubtitleMapping.Status.class, new StatusRenderer());

		table.setRowHeight(28);
		table.setIntercellSpacing(new Dimension(5, 5));

		table.setBackground(Color.white);
		table.setAutoCreateRowSorter(true);
		table.setFillsViewportHeight(true);

		LanguageComboBox languageEditor = new LanguageComboBox(Language.getLanguage("en"), null);

		// disable selection
		table.setSelectionModel(new EmptySelectionModel());
		languageEditor.setFocusable(false);

		table.setDefaultEditor(Language.class, new DefaultCellEditor(languageEditor) {

			@Override
			public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
				LanguageComboBox editor = (LanguageComboBox) super.getTableCellEditorComponent(table, value, isSelected, row, column);
				editor.getModel().setSelectedItem(value);
				return editor;
			}
		});

		table.setDefaultEditor(Movie.class, new TableCellEditor() {

			@Override
			public boolean stopCellEditing() {
				return true;
			}

			@Override
			public boolean shouldSelectCell(EventObject evt) {
				return false;
			}

			@Override
			public void removeCellEditorListener(CellEditorListener listener) {
			}

			@Override
			public boolean isCellEditable(EventObject evt) {
				return true;
			}

			@Override
			public Object getCellEditorValue() {
				return null;
			}

			@Override
			public void cancelCellEditing() {
			}

			@Override
			public void addCellEditorListener(CellEditorListener evt) {
			}

			@Override
			public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
				table.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				try {
					SubtitleMappingTableModel model = (SubtitleMappingTableModel) table.getModel();
					SubtitleMapping mapping = model.getData()[table.convertRowIndexToModel(row)];

					File video = mapping.getVideo() != null ? mapping.getVideo() : mapping.getSubtitle();
					String query = FileUtilities.getName(video);

					// check if query contain an episode identifier
					SeriesNameMatcher snm = new SeriesNameMatcher();
					String sn = snm.matchByEpisodeIdentifier(query);
					if (sn != null) {
						query = sn;
					}

					String input = showInputDialog("Enter movie / series name:", stripReleaseInfo(query), getStructurePathTail(video).getPath(), SubtitleUploadDialog.this);
					if (input != null && input.length() > 0) {
						List<SubtitleSearchResult> options = database.searchIMDB(input);
						if (options.size() > 0) {
							SelectDialog<Movie> dialog = new SelectDialog<Movie>(SubtitleUploadDialog.this, options);
							dialog.setLocation(getOffsetLocation(dialog.getOwner()));
							dialog.setVisible(true);
							Movie selectedValue = dialog.getSelectedValue();
							if (selectedValue != null) {
								mapping.setIdentity(selectedValue);
								if (mapping.getIdentity() != null && mapping.getLanguage() != null) {
									mapping.setForceIdentity(true);
									mapping.setState(SubtitleMapping.Status.CheckPending);
									startChecking();
								}
							}
						}
					}
				} catch (Exception e) {
					Logger.getLogger(SubtitleUploadDialog.class.getClass().getName()).log(Level.WARNING, e.getMessage(), e);
				}
				table.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				return null;
			}
		});

		table.setDefaultEditor(File.class, new TableCellEditor() {

			@Override
			public boolean stopCellEditing() {
				return true;
			}

			@Override
			public boolean shouldSelectCell(EventObject evt) {
				return false;
			}

			@Override
			public void removeCellEditorListener(CellEditorListener listener) {
			}

			@Override
			public boolean isCellEditable(EventObject evt) {
				return true;
			}

			@Override
			public Object getCellEditorValue() {
				return null;
			}

			@Override
			public void cancelCellEditing() {
			}

			@Override
			public void addCellEditorListener(CellEditorListener evt) {
			}

			@Override
			public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
				SubtitleMappingTableModel model = (SubtitleMappingTableModel) table.getModel();
				SubtitleMapping mapping = model.getData()[table.convertRowIndexToModel(row)];

				List<File> files = showLoadDialogSelectFiles(false, false, mapping.getSubtitle().getParentFile(), VIDEO_FILES, "Select Video File", new ActionEvent(table, ActionEvent.ACTION_PERFORMED, "Select"));
				if (files.size() > 0) {
					mapping.setVideo(files.get(0));
					mapping.setState(SubtitleMapping.Status.CheckPending);
					startChecking();
				}
				return null;
			}
		});

		return table;
	}

	public void setUploadPlan(Map<File, File> uploadPlan) {
		List<SubtitleMapping> mappings = new ArrayList<SubtitleMapping>(uploadPlan.size());
		for (Entry<File, File> entry : uploadPlan.entrySet()) {
			File subtitle = entry.getKey();
			File video = entry.getValue();

			Locale locale = MediaDetection.guessLanguageFromSuffix(subtitle);
			Language language = Language.getLanguage(locale);

			mappings.add(new SubtitleMapping(subtitle, video, language));
		}

		subtitleMappingTable.setModel(new SubtitleMappingTableModel(mappings.toArray(new SubtitleMapping[0])));
	}

	public void startChecking() {
		SubtitleMapping[] data = ((SubtitleMappingTableModel) subtitleMappingTable.getModel()).getData();
		for (SubtitleMapping it : data) {
			if (it.getSubtitle() != null && it.getVideo() != null) {
				if (it.getStatus() == SubtitleMapping.Status.CheckPending) {
					checkExecutorService.submit(new CheckTask(it));
				}
			} else {
				it.setState(SubtitleMapping.Status.IllegalInput);
			}
		}
	}

	private final Action uploadAction = new AbstractAction("Upload", ResourceManager.getIcon("dialog.continue")) {

		@Override
		public void actionPerformed(ActionEvent evt) {
			// disable any active cell editor
			if (subtitleMappingTable.getCellEditor() != null) {
				subtitleMappingTable.getCellEditor().stopCellEditing();
			}

			// don't allow restart of upload as long as there are still unfinished download tasks
			if (uploadExecutorService != null && !uploadExecutorService.isTerminated()) {
				return;
			}

			uploadExecutorService = Executors.newSingleThreadExecutor();

			SubtitleMapping[] data = ((SubtitleMappingTableModel) subtitleMappingTable.getModel()).getData();
			for (final SubtitleMapping it : data) {
				if (it.getStatus() == SubtitleMapping.Status.UploadReady) {
					uploadExecutorService.submit(new UploadTask(it));
				}
			}

			// terminate after all uploads have been completed
			uploadExecutorService.shutdown();
		}
	};

	private final Action finishAction = new AbstractAction("Close", ResourceManager.getIcon("dialog.cancel")) {

		@Override
		public void actionPerformed(ActionEvent evt) {
			if (checkExecutorService != null) {
				checkExecutorService.shutdownNow();
			}
			if (uploadExecutorService != null) {
				uploadExecutorService.shutdownNow();
			}

			setVisible(false);
			dispose();
		}
	};

	private class MovieRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			String text = null;
			String tooltip = null;
			Icon icon = null;

			Movie movie = (Movie) value;
			if (movie != null) {
				text = movie.toString();
				tooltip = String.format("%s [tt%07d]", movie.toString(), movie.getImdbId());
				icon = database.getIcon();
			}

			setText(text);
			setToolTipText(tooltip);
			setIcon(icon);
			return this;
		}
	}

	private class FileRenderer extends DefaultTableCellRenderer {

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			String text = null;
			String tooltip = null;
			Icon icon = null;

			if (value != null) {
				File file = (File) value;
				text = file.getName();
				tooltip = file.getPath();
				if (SUBTITLE_FILES.accept(file)) {
					icon = ResourceManager.getIcon("file.subtitle");
				} else if (VIDEO_FILES.accept(file)) {
					icon = ResourceManager.getIcon("file.video");
				}
			}

			setText(text);
			setToolTipText(text);
			setIcon(icon);
			return this;
		}
	}

	private class LanguageRenderer implements TableCellRenderer, ListCellRenderer {

		private DefaultTableCellRenderer tableCell = new DefaultTableCellRenderer();
		private DefaultListCellRenderer listCell = new DefaultListCellRenderer();

		private Component configure(JLabel c, Object value, boolean isSelected, boolean hasFocus) {
			String text = null;
			Icon icon = null;

			if (value != null) {
				Language language = (Language) value;
				text = language.getName();
				icon = ResourceManager.getFlagIcon(language.getCode());
			}

			c.setText(text);
			c.setIcon(icon);
			return c;
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			return configure((DefaultTableCellRenderer) tableCell.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column), value, isSelected, hasFocus);
		}

		@Override
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			return configure((DefaultListCellRenderer) listCell.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus), value, isSelected, cellHasFocus);
		}
	}

	private class StatusRenderer extends DefaultTableCellRenderer {

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			String text = null;
			Icon icon = null;

			// CheckPending, Checking, CheckFailed, AlreadyExists, Identifying, IdentificationRequired, UploadPending, Uploading, UploadComplete, UploadFailed;
			switch ((SubtitleMapping.Status) value) {
			case IllegalInput:
				text = "No video/subtitle pair";
				icon = ResourceManager.getIcon("status.error");
				break;
			case CheckPending:
				text = "Pending...";
				icon = ResourceManager.getIcon("worker.pending");
				break;
			case Checking:
				text = "Checking database...";
				icon = ResourceManager.getIcon("database.go");
				break;
			case CheckFailed:
				text = "Failed to check database";
				icon = ResourceManager.getIcon("database.error");
				break;
			case AlreadyExists:
				text = "Subtitle already exists in database";
				icon = ResourceManager.getIcon("database.ok");
				break;
			case Identifying:
				text = "Auto-detect missing information";
				icon = ResourceManager.getIcon("action.export");
				break;
			case IdentificationRequired:
				text = "Please input the missing information";
				icon = ResourceManager.getIcon("dialog.continue.invalid");
				break;
			case UploadReady:
				text = "Ready for upload";
				icon = ResourceManager.getIcon("dialog.continue");
				break;
			case Uploading:
				text = "Uploading...";
				icon = ResourceManager.getIcon("database.go");
				break;
			case UploadComplete:
				text = "Upload successful";
				icon = ResourceManager.getIcon("database.ok");
				break;
			case UploadFailed:
				text = "Upload failed";
				icon = ResourceManager.getIcon("database.error");
				break;
			}

			setText(text);
			setIcon(icon);
			return this;
		}
	}

	private class SubtitleMappingTableModel extends AbstractTableModel {

		private final SubtitleMapping[] data;

		public SubtitleMappingTableModel(SubtitleMapping... mappings) {
			this.data = mappings.clone();

			for (int i = 0; i < data.length; i++) {
				data[i].addPropertyChangeListener(new SubtitleMappingListener(i));
			}
		}

		public SubtitleMapping[] getData() {
			return data.clone();
		}

		@Override
		public int getColumnCount() {
			return 5;
		}

		@Override
		public String getColumnName(int column) {
			switch (column) {
			case 0:
				return "Movie / Series";
			case 1:
				return "Video";
			case 2:
				return "Subtitle";
			case 3:
				return "Language";
			case 4:
				return "Status";
			}
			return null;
		}

		@Override
		public int getRowCount() {
			return data.length;
		}

		@Override
		public Object getValueAt(int row, int column) {
			switch (column) {
			case 0:
				return data[row].getIdentity();
			case 1:
				return data[row].getVideo();
			case 2:
				return data[row].getSubtitle();
			case 3:
				return data[row].getLanguage();
			case 4:
				return data[row].getStatus();
			}
			return null;
		}

		@Override
		public void setValueAt(Object value, int row, int column) {
			if (getColumnClass(column) == Language.class && value instanceof Language) {
				data[row].setLanguage((Language) value);

				if (data[row].getStatus() == SubtitleMapping.Status.IdentificationRequired) {
					data[row].setState(SubtitleMapping.Status.CheckPending);
					startChecking();
				}
			}
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			return (column == 0 || column == 1 || column == 3) && EnumSet.of(SubtitleMapping.Status.IdentificationRequired, SubtitleMapping.Status.UploadReady, SubtitleMapping.Status.IllegalInput).contains(data[row].getStatus());
		}

		@Override
		public Class<?> getColumnClass(int column) {
			switch (column) {
			case 0:
				return Movie.class;
			case 1:
				return File.class;
			case 2:
				return File.class;
			case 3:
				return Language.class;
			case 4:
				return SubtitleMapping.Status.class;
			}

			return null;
		}

		private class SubtitleMappingListener implements PropertyChangeListener {

			private final int index;

			public SubtitleMappingListener(int index) {
				this.index = index;
			}

			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				// update state and subtitle options
				fireTableRowsUpdated(index, index);
			}
		}
	}

	private static class SubtitleMapping extends AbstractBean {

		enum Status {
			IllegalInput, CheckPending, Checking, CheckFailed, AlreadyExists, Identifying, IdentificationRequired, UploadReady, Uploading, UploadComplete, UploadFailed;
		}

		private Object identity;
		private Object remoteIdentity;
		private File subtitle;
		private File video;
		private Language language;

		private Status status = Status.CheckPending;
		private String message = null;
		private boolean forceIdentity = false;

		public SubtitleMapping(File subtitle, File video, Language language) {
			this.subtitle = subtitle;
			this.video = video;
			this.language = language;
		}

		public Object getIdentity() {
			return identity;
		}

		public Object getRemoteIdentity() {
			return remoteIdentity;
		}

		public File getSubtitle() {
			return subtitle;
		}

		public File getVideo() {
			return video;
		}

		public Language getLanguage() {
			return language;
		}

		public Status getStatus() {
			return status;
		}

		public void setVideo(File video) {
			this.video = video;
			firePropertyChange("video", null, this.video);
		}

		public void setIdentity(Object identity) {
			this.identity = identity;
			firePropertyChange("identity", null, this.identity);
		}

		public void setRemoteIdentity(Object identity) {
			this.remoteIdentity = identity;
			firePropertyChange("remoteIdentity", null, this.remoteIdentity);
		}

		public void setLanguage(Language language) {
			this.language = language;
			firePropertyChange("language", null, this.language);
		}

		public void setState(Status status) {
			this.status = status;
			firePropertyChange("status", null, this.status);
		}

		public boolean getForceIdentity() {
			return this.forceIdentity;
		}

		public void setForceIdentity(boolean forceIdentity) {
			this.forceIdentity = forceIdentity;
		}
	}

	private class CheckTask extends SwingWorker<Object, Void> {

		private final SubtitleMapping mapping;

		public CheckTask(SubtitleMapping mapping) {
			this.mapping = mapping;
		}

		@Override
		protected Object doInBackground() throws Exception {
			try {
				CheckResult checkResult = null;

				if (!mapping.getForceIdentity()) {
					mapping.setState(SubtitleMapping.Status.Checking);

					checkResult = database.checkSubtitle(mapping.getVideo(), mapping.getSubtitle());

					if (checkResult.exists) {
						mapping.setRemoteIdentity(checkResult.identity);
						mapping.setLanguage(Language.getLanguage(checkResult.language)); // trust language hint only if upload not required

						// force upload all subtitles regardless of what TryUploadSubtitles returns (because other programs often submit crap)
						// mapping.setState(SubtitleMapping.Status.AlreadyExists);
					}
				}

				if (mapping.getLanguage() == null) {
					mapping.setState(SubtitleMapping.Status.Identifying);
					try {
						Locale locale = database.detectLanguage(FileUtilities.readFile(mapping.getSubtitle()));
						mapping.setLanguage(Language.getLanguage(locale));
					} catch (Exception e) {
						Logger.getLogger(CheckTask.class.getClass().getName()).log(Level.WARNING, "Failed to auto-detect language: " + e.getMessage());
					}
				}

				if (mapping.getIdentity() == null) {
					mapping.setState(SubtitleMapping.Status.Identifying);
					try {
						if (MediaDetection.isEpisode(mapping.getVideo().getPath(), true)) {
							List<String> seriesNames = MediaDetection.detectSeriesNames(Collections.singleton(mapping.getVideo()), true, false, Locale.ENGLISH);
							NAMES: for (String name : seriesNames) {
								List<SearchResult> options = WebServices.TheTVDB.search(name, Locale.ENGLISH);
								for (SearchResult entry : options) {
									TheTVDBSeriesInfo seriesInfo = (TheTVDBSeriesInfo) WebServices.TheTVDB.getSeriesInfo(entry, Locale.ENGLISH);
									if (seriesInfo.getImdbId() != null) {
										int imdbId = grepImdbId(seriesInfo.getImdbId()).iterator().next();
										mapping.setIdentity(WebServices.OpenSubtitles.getMovieDescriptor(new Movie(null, 0, imdbId, -1), Locale.ENGLISH));
										break NAMES;
									}
								}
							}
						} else {
							Collection<Movie> identity = MediaDetection.detectMovie(mapping.getVideo(), database, Locale.ENGLISH, true);
							for (Movie it : identity) {
								if (it.getImdbId() <= 0 && it.getTmdbId() > 0) {
									it = WebServices.TheMovieDB.getMovieDescriptor(it, Locale.ENGLISH);
								}
								if (it != null && it.getImdbId() > 0) {
									mapping.setIdentity(it);
									break;
								}
							}
						}
					} catch (Exception e) {
						Logger.getLogger(CheckTask.class.getClass().getName()).log(Level.WARNING, "Failed to auto-detect movie: " + e.getMessage());
					}
				}

				if (mapping.getIdentity() == null || mapping.getLanguage() == null) {
					mapping.setState(SubtitleMapping.Status.IdentificationRequired);
				} else {
					mapping.setState(SubtitleMapping.Status.UploadReady);
				}

				return checkResult;
			} catch (Exception e) {
				Logger.getLogger(CheckTask.class.getClass().getName()).log(Level.SEVERE, e.getMessage(), e);
				mapping.setState(SubtitleMapping.Status.CheckFailed);
			}
			return null;
		}
	}

	private class UploadTask extends SwingWorker<Object, Void> {

		private final SubtitleMapping mapping;

		public UploadTask(SubtitleMapping mapping) {
			this.mapping = mapping;
		}

		@Override
		protected Object doInBackground() {
			try {
				mapping.setState(SubtitleMapping.Status.Uploading);

				database.uploadSubtitle(mapping.getIdentity(), mapping.getLanguage().getLocale(), mapping.getVideo(), mapping.getSubtitle());
				mapping.setState(SubtitleMapping.Status.UploadComplete);
			} catch (Exception e) {
				Logger.getLogger(UploadTask.class.getClass().getName()).log(Level.SEVERE, e.getMessage(), e);
				mapping.setState(SubtitleMapping.Status.UploadFailed);
			}
			return null;
		}
	}

}
