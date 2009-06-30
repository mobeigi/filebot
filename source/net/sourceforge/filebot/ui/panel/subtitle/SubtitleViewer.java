
package net.sourceforge.filebot.ui.panel.subtitle;


import static java.awt.Font.*;
import static java.util.Collections.*;
import static java.util.regex.Pattern.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import net.miginfocom.swing.MigLayout;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.subtitle.SubtitleElement;
import net.sourceforge.tuned.ui.GradientStyle;
import net.sourceforge.tuned.ui.LazyDocumentListener;
import net.sourceforge.tuned.ui.notification.SeparatorBorder;
import net.sourceforge.tuned.ui.notification.SeparatorBorder.Position;


class SubtitleViewer extends JFrame {
	
	private final JLabel titleLabel = new JLabel();
	
	private final JLabel infoLabel = new JLabel();
	
	private final SubtitleTableModel model = new SubtitleTableModel();
	
	private final JTextField filterEditor = new JTextField();
	
	private final JTable subtitleTable = createTable(model);
	

	public SubtitleViewer(String title) {
		super(title);
		
		// bold title label in header
		titleLabel.setText(title);
		titleLabel.setFont(titleLabel.getFont().deriveFont(BOLD));
		
		JPanel header = new JPanel(new MigLayout("insets dialog, nogrid, fillx"));
		
		header.setBackground(Color.white);
		header.setBorder(new SeparatorBorder(1, new Color(0xB4B4B4), new Color(0xACACAC), GradientStyle.LEFT_TO_RIGHT, Position.BOTTOM));
		
		header.add(titleLabel, "wrap");
		header.add(infoLabel, "gap indent*2, wrap paragraph:push");
		
		JPanel content = new JPanel(new MigLayout("fill, insets dialog, nogrid", "[fill]", "[pref!][fill]"));
		
		content.add(new JLabel("Filter:"), "gap indent:push");
		content.add(filterEditor, "wmin 120px, gap rel");
		content.add(new JButton(clearFilterAction), "w 24px!, h 24px!, wrap");
		content.add(new JScrollPane(subtitleTable), "grow");
		
		JComponent pane = (JComponent) getContentPane();
		pane.setLayout(new MigLayout("fill, insets 0"));
		
		pane.add(header, "hmin 20px, growx, dock north");
		pane.add(content, "grow");
		
		// initialize selection modes
		subtitleTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		
		final DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.ROOT);
		timeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		
		// change time stamp format
		subtitleTable.setDefaultRenderer(Date.class, new DefaultTableCellRenderer() {
			
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				return super.getTableCellRendererComponent(table, timeFormat.format(value), isSelected, hasFocus, row, column);
			}
		});
		
		// change text format
		subtitleTable.setDefaultRenderer(String.class, new DefaultTableCellRenderer() {
			
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				return super.getTableCellRendererComponent(table, value.toString().replaceAll("\\s+", " "), isSelected, hasFocus, row, column);
			}
		});
		
		// update sequence and element filter on change
		filterEditor.getDocument().addDocumentListener(new LazyDocumentListener() {
			
			@Override
			public void update(DocumentEvent e) {
				List<SubtitleFilter> filterList = new ArrayList<SubtitleFilter>();
				
				// filter by all words
				for (String word : filterEditor.getText().split("\\s+")) {
					filterList.add(new SubtitleFilter(word));
				}
				
				TableRowSorter<?> sorter = (TableRowSorter<?>) subtitleTable.getRowSorter();
				sorter.setRowFilter(RowFilter.andFilter(filterList));
			}
		});
		
		// initialize window properties
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setLocationByPlatform(true);
		setResizable(true);
		pack();
	}
	

	public void setData(List<SubtitleElement> data) {
		model.setData(data);
	}
	

	private JTable createTable(TableModel model) {
		JTable table = new JTable(model);
		table.setAutoCreateRowSorter(true);
		table.setFillsViewportHeight(true);
		table.setRowHeight(18);
		
		// decrease column width for the row number columns
		DefaultTableColumnModel m = ((DefaultTableColumnModel) table.getColumnModel());
		m.getColumn(0).setMaxWidth(40);
		m.getColumn(1).setMaxWidth(60);
		m.getColumn(2).setMaxWidth(60);
		
		return table;
	}
	

	public JLabel getTitleLabel() {
		return titleLabel;
	}
	

	public JLabel getInfoLabel() {
		return infoLabel;
	}
	

	private final Action clearFilterAction = new AbstractAction(null, ResourceManager.getIcon("edit.clear")) {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			filterEditor.setText("");
		}
	};
	

	private static class SubtitleFilter extends RowFilter<Object, Integer> {
		
		private final Pattern filter;
		

		public SubtitleFilter(String filter) {
			this.filter = compile(quote(filter), CASE_INSENSITIVE | UNICODE_CASE | CANON_EQ);
		}
		

		@Override
		public boolean include(Entry<?, ? extends Integer> entry) {
			SubtitleTableModel model = (SubtitleTableModel) entry.getModel();
			SubtitleElement element = model.getRow(entry.getIdentifier());
			
			return filter.matcher(element.getText()).find();
		}
		
	}
	

	private static class SubtitleTableModel extends AbstractTableModel {
		
		private List<SubtitleElement> data = emptyList();
		

		public void setData(List<SubtitleElement> data) {
			this.data = new ArrayList<SubtitleElement>(data);
			
			// update view
			fireTableDataChanged();
		}
		

		public SubtitleElement getRow(int row) {
			return data.get(row);
		}
		

		@Override
		public String getColumnName(int column) {
			switch (column) {
				case 0:
					return "#";
				case 1:
					return "Start";
				case 2:
					return "End";
				case 3:
					return "Text";
				default:
					return null;
			}
		}
		

		@Override
		public int getColumnCount() {
			return 4;
		}
		

		@Override
		public int getRowCount() {
			return data.size();
		}
		

		@Override
		public Class<?> getColumnClass(int column) {
			switch (column) {
				case 0:
					return Integer.class;
				case 1:
					return Date.class;
				case 2:
					return Date.class;
				case 3:
					return String.class;
				default:
					return null;
			}
		}
		

		@Override
		public Object getValueAt(int row, int column) {
			switch (column) {
				case 0:
					return row + 1;
				case 1:
					return getRow(row).getStart();
				case 2:
					return getRow(row).getEnd();
				case 3:
					return getRow(row).getText();
				default:
					return null;
			}
		}
	}
	
}
