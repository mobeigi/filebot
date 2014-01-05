package net.sourceforge.filebot.ui.analyze;

import static net.sourceforge.filebot.MediaTypes.*;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import net.miginfocom.swing.MigLayout;
import net.sourceforge.filebot.media.MetaAttributes;
import net.sourceforge.filebot.ui.analyze.FileTree.FolderNode;
import net.sourceforge.filebot.web.Episode;
import net.sourceforge.filebot.web.Movie;
import net.sourceforge.filebot.web.SearchResult;
import net.sourceforge.tuned.ui.LoadingOverlayPane;

class AttributeTool extends Tool<TableModel> {

	private JTable table = new JTable(new FileAttributesTableModel());

	public AttributeTool() {
		super("Attributes");

		table.setAutoCreateRowSorter(true);
		table.setFillsViewportHeight(true);
		table.setBackground(Color.white);
		table.setRowHeight(20);

		JScrollPane scrollPane = new JScrollPane(table);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());

		setLayout(new MigLayout("insets 0, fill"));
		add(new LoadingOverlayPane(scrollPane, this), "grow");
	}

	@Override
	protected TableModel createModelInBackground(FolderNode sourceModel) throws InterruptedException {
		FileAttributesTableModel model = new FileAttributesTableModel();

		for (Iterator<File> iterator = sourceModel.fileIterator(); iterator.hasNext();) {
			File file = iterator.next();

			if (VIDEO_FILES.accept(file)) {
				try {
					MetaAttributes attributes = new MetaAttributes(file);
					String metaId = null;
					Object metaObject = null;
					String originalName = null;

					try {
						originalName = attributes.getOriginalName();
						metaObject = attributes.getObject();

						String format = "%s::%d";
						if (metaObject instanceof Episode) {
							Object seriesObject = ((Episode) metaObject).getSeries();
							if (seriesObject != null) {
								String type = seriesObject.getClass().getSimpleName().replace(SearchResult.class.getSimpleName(), "");
								Integer code = (Integer) seriesObject.getClass().getMethod("getId").invoke(seriesObject);
								metaId = String.format(format, type, code);
							}
						} else if (metaObject instanceof Movie) {
							Movie movie = (Movie) metaObject;
							if (movie.getTmdbId() > 0) {
								metaId = String.format(format, "TheMovieDB", movie.getTmdbId());
							} else if (movie.getImdbId() > 0) {
								metaId = String.format(format, "IMDB", movie.getImdbId());
							}
						}
					} catch (Exception e) {
						// ignore
					}

					model.addRow(metaId, metaObject, originalName, file);
				} catch (Exception e) {
					Logger.getLogger(AttributeTool.class.getName()).log(Level.WARNING, e.getMessage());
				}
			}
		}

		return model;
	}

	@Override
	protected void setModel(TableModel model) {
		table.setModel(model);
	}

	private static class FileAttributesTableModel extends AbstractTableModel {

		private final List<Object[]> rows = new ArrayList<Object[]>();

		public boolean addRow(Object... row) {
			if (row.length != getColumnCount())
				return false;

			return rows.add(row);
		}

		@Override
		public int getColumnCount() {
			return 4;
		}

		@Override
		public String getColumnName(int column) {
			switch (column) {
			case 0:
				return "Meta ID";
			case 1:
				return "Meta Attributes";
			case 2:
				return "Original Name";
			case 3:
				return "File Path";
			}
			return null;
		}

		@Override
		public int getRowCount() {
			return rows.size();
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			return rows.get(rowIndex)[columnIndex];
		}

	}

}
