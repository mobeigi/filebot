package net.filebot.ui.subtitle.upload;

import static net.filebot.media.MediaDetection.*;
import static net.filebot.ui.NotificationLogging.*;
import static net.filebot.util.ui.SwingUI.*;

import java.awt.Component;
import java.awt.Cursor;
import java.io.File;
import java.util.EventObject;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;

import net.filebot.similarity.SeriesNameMatcher;
import net.filebot.ui.SelectDialog;
import net.filebot.util.FileUtilities;
import net.filebot.web.Movie;
import net.filebot.web.OpenSubtitlesClient;
import net.filebot.web.SubtitleSearchResult;

class MovieEditor implements TableCellEditor {

	private OpenSubtitlesClient database;

	public MovieEditor(OpenSubtitlesClient database) {
		this.database = database;
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		try {
			getWindow(table).setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

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

			final String input = showInputDialog("Enter movie / series name:", stripReleaseInfo(query), getStructurePathTail(video).getPath(), table);
			if (input != null && input.length() > 0) {
				SwingWorker<List<SubtitleSearchResult>, Void> worker = new SwingWorker<List<SubtitleSearchResult>, Void>() {

					@Override
					protected List<SubtitleSearchResult> doInBackground() throws Exception {
						return database.searchIMDB(input);
					}

					@Override
					protected void done() {
						try {
							List<SubtitleSearchResult> options = get();
							if (options.size() > 0) {
								SelectDialog<Movie> dialog = new SelectDialog<Movie>(table, options);
								dialog.setLocation(getOffsetLocation(dialog.getOwner()));
								dialog.setVisible(true);
								Movie selectedValue = dialog.getSelectedValue();
								if (selectedValue != null) {
									mapping.setIdentity(selectedValue);
									if (mapping.getIdentity() != null && mapping.getLanguage() != null && mapping.getVideo() != null) {
										mapping.setState(Status.CheckPending);
									}
								}
							} else {
								UILogger.warning(String.format("%s: \"%s\" has not been found", database.getName(), input));
							}
						} catch (Exception e) {
							Logger.getLogger(SubtitleUploadDialog.class.getClass().getName()).log(Level.WARNING, e.getMessage(), e);
						}
						getWindow(table).setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
					};
				};
				worker.execute();
			} else {
				getWindow(table).setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			}
		} catch (Exception e) {
			Logger.getLogger(SubtitleUploadDialog.class.getClass().getName()).log(Level.WARNING, e.toString());
			getWindow(table).setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
		return null;
	}

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

}
