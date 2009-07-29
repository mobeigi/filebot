
package net.sourceforge.filebot.ui.panel.rename;


import static net.sourceforge.tuned.ui.TunedUtilities.*;

import java.awt.Window;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import net.miginfocom.swing.MigLayout;
import net.sourceforge.filebot.mediainfo.MediaInfo;
import net.sourceforge.filebot.mediainfo.MediaInfo.StreamKind;


class MediaInfoPane extends JTabbedPane {
	
	public MediaInfoPane(File file) {
		// get media info
		MediaInfo mediaInfo = new MediaInfo();
		
		if (!mediaInfo.open(file))
			throw new IllegalArgumentException("Cannot open file: " + file);
		
		// create tab for each stream
		for (Entry<StreamKind, List<Map<String, String>>> entry : mediaInfo.snapshot().entrySet()) {
			for (Map<String, String> parameters : entry.getValue()) {
				addTableTab(entry.getKey().toString(), parameters);
			}
		}
		
		mediaInfo.close();
	}
	

	public void addTableTab(String title, Map<String, String> data) {
		JTable table = new JTable(new ParameterTableModel(data));
		table.setFillsViewportHeight(true);
		
		// allow sorting
		table.setAutoCreateRowSorter(true);
		
		// sort by parameter name
		table.getRowSorter().toggleSortOrder(0);
		
		addTab(title, new JScrollPane(table));
	}
	

	public static void showMessageDialog(Window parent, File file) {
		final JDialog dialog = new JDialog(parent, "MediaInfo", ModalityType.DOCUMENT_MODAL);
		
		Action closeAction = new AbstractAction("OK") {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				dialog.setVisible(false);
				dialog.dispose();
			}
		};
		
		JComponent c = (JComponent) dialog.getContentPane();
		c.setLayout(new MigLayout("fill", "[align center]", "[fill][pref!]"));
		c.add(new MediaInfoPane(file), "grow, wrap");
		c.add(new JButton(closeAction), "wmin 80px, hmin 25px");
		
		dialog.setLocation(getPreferredLocation(dialog));
		dialog.pack();
		
		dialog.setVisible(true);
	}
	

	private static class ParameterTableModel extends AbstractTableModel {
		
		private final List<Entry<String, String>> data;
		

		public ParameterTableModel(Map<String, String> data) {
			this.data = new ArrayList<Entry<String, String>>(data.entrySet());
		}
		

		@Override
		public int getRowCount() {
			return data.size();
		}
		

		@Override
		public int getColumnCount() {
			return 2;
		}
		

		@Override
		public String getColumnName(int column) {
			switch (column) {
				case 0:
					return "Parameter";
				case 1:
					return "Value";
			}
			
			return null;
		}
		

		@Override
		public Object getValueAt(int row, int column) {
			switch (column) {
				case 0:
					return data.get(row).getKey();
				case 1:
					return data.get(row).getValue();
			}
			
			return null;
		}
	}
}
