package net.filebot.ui.analyze;

import static net.filebot.Logging.*;
import static net.filebot.MediaTypes.*;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import net.filebot.media.MetaAttributes;
import net.filebot.util.FileUtilities;
import net.filebot.util.ui.LoadingOverlayPane;
import net.filebot.web.Episode;
import net.filebot.web.Movie;
import net.filebot.web.SeriesInfo;
import net.miginfocom.swing.MigLayout;

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
	protected TableModel createModelInBackground(File root) throws InterruptedException {
		List<File> files = (root != null) ? FileUtilities.listFiles(root) : new ArrayList<File>();

		FileAttributesTableModel model = new FileAttributesTableModel();
		for (File file : files) {
			if (VIDEO_FILES.accept(file)) {
				try {
					MetaAttributes attributes = new MetaAttributes(file);
					String metaId = null;
					Object metaObject = null;
					String originalName = null;

					try {
						originalName = attributes.getOriginalName();
						metaObject = attributes.getObject();

						if (metaObject instanceof Episode) {
							SeriesInfo seriesInfo = ((Episode) metaObject).getSeriesInfo();
							if (seriesInfo != null) {
								metaId = String.format("%s::%d", seriesInfo.getDatabase(), seriesInfo.getId());
							}
						} else if (metaObject instanceof Movie) {
							Movie movie = (Movie) metaObject;
							if (movie.getTmdbId() > 0) {
								metaId = String.format("%s::%d", "TheMovieDB", movie.getTmdbId());
							} else if (movie.getImdbId() > 0) {
								metaId = String.format("%s::%d", "OMDb", movie.getImdbId());
							}
						}
					} catch (Exception e) {
						// ignore
					}

					model.addRow(metaId, metaObject, originalName, file);
				} catch (Exception e) {
					debug.warning("Failed to read xattr: " + e);
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
