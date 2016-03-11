package net.filebot.ui.analyze;

import static java.util.Collections.*;
import static net.filebot.Logging.*;
import static net.filebot.MediaTypes.*;
import static net.filebot.util.FileUtilities.*;

import java.awt.Color;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.IntStream;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import net.filebot.mediainfo.MediaInfo;
import net.filebot.mediainfo.MediaInfo.StreamKind;
import net.filebot.util.FileUtilities;
import net.filebot.util.ui.LoadingOverlayPane;
import net.miginfocom.swing.MigLayout;

class MediaInfoTool extends Tool<TableModel> {

	private JTable table = new JTable(new MediaInfoTableModel());

	public MediaInfoTool() {
		super("MediaInfo");

		table.setAutoCreateRowSorter(true);
		table.setAutoCreateColumnsFromModel(true);
		table.setFillsViewportHeight(true);

		table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
		table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		table.setBackground(Color.white);
		table.setGridColor(new Color(0xEEEEEE));
		table.setRowHeight(25);

		JScrollPane scrollPane = new JScrollPane(table);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());

		setLayout(new MigLayout("insets 0, fill"));
		add(new LoadingOverlayPane(scrollPane, this), "grow");
	}

	@Override
	protected TableModel createModelInBackground(File root) throws InterruptedException {
		if (root == null) {
			return new MediaInfoTableModel();
		}

		List<File> files = filter(FileUtilities.listFiles(root), VIDEO_FILES, AUDIO_FILES);
		Map<MediaInfoKey, String[]> data = new TreeMap<>();

		try (MediaInfo mi = new MediaInfo()) {
			IntStream.range(0, files.size()).forEach(f -> {
				try {
					mi.open(files.get(f));
					mi.snapshot().forEach((kind, streams) -> {
						IntStream.range(0, streams.size()).forEach(i -> {
							streams.get(i).forEach((name, value) -> {
								String[] values = data.computeIfAbsent(new MediaInfoKey(kind, i, name), k -> new String[files.size()]);
								values[f] = value;
							});
						});
					});
				} catch (Exception e) {
					debug.warning(e.getMessage());
				}
			});
		}

		return new MediaInfoTableModel(files, data);
	}

	@Override
	protected void setModel(TableModel model) {
		table.setModel(model);
	}

	private static class MediaInfoKey implements Comparable<MediaInfoKey> {

		public final StreamKind kind;
		public final int stream;
		public final String name;

		public MediaInfoKey(StreamKind kind, int stream, String name) {
			this.kind = kind;
			this.stream = stream;
			this.name = name;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof MediaInfoKey) {
				MediaInfoKey other = (MediaInfoKey) obj;
				return kind == other.kind && stream == other.stream && name.equals(other.name);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return kind.ordinal() + (stream << 8) + name.hashCode();
		}

		@Override
		public int compareTo(MediaInfoKey other) {
			if (kind != other.kind)
				return kind.compareTo(other.kind);
			if (stream != other.stream)
				return Integer.compare(stream, other.stream);
			else
				return name.compareTo(other.name);
		}

	}

	private static class MediaInfoTableModel extends AbstractTableModel {

		private final File[] files;
		private final MediaInfoKey[] keys;
		private final String[][] rows;

		public MediaInfoTableModel() {
			this(emptyList(), emptyMap());
		}

		public MediaInfoTableModel(List<File> files, Map<MediaInfoKey, String[]> values) {
			this.files = files.toArray(new File[0]);
			this.keys = values.keySet().toArray(new MediaInfoKey[0]);
			this.rows = values.values().toArray(new String[0][]);
		}

		@Override
		public int getColumnCount() {
			return files.length + 3;
		}

		@Override
		public String getColumnName(int column) {
			switch (column) {
			case 0:
				return "Stream";
			case 1:
				return "#";
			case 2:
				return "Property";
			default:
				return files[column - 3].getName();
			}
		}

		@Override
		public Class<?> getColumnClass(int column) {
			switch (column) {
			case 0:
				return StreamKind.class;
			case 1:
				return Integer.class;
			default:
				return String.class;
			}
		}

		@Override
		public int getRowCount() {
			return keys.length;
		}

		@Override
		public Object getValueAt(int row, int column) {
			switch (column) {
			case 0:
				return keys[row].kind;
			case 1:
				return keys[row].stream;
			case 2:
				return keys[row].name;
			default:
				return rows[row][column - 3];
			}
		}

	}

}
