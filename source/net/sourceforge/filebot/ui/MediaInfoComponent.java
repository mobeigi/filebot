
package net.sourceforge.filebot.ui;


import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.AbstractAction;
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
import net.sourceforge.tuned.ui.TunedUtilities;


public class MediaInfoComponent extends JTabbedPane {
	
	public MediaInfoComponent(Map<StreamKind, List<Map<String, String>>> mediaInfo) {
		insert(mediaInfo);
	}
	

	public void insert(Map<StreamKind, List<Map<String, String>>> mediaInfo) {
		// create tabs for all streams
		for (Entry<StreamKind, List<Map<String, String>>> entry : mediaInfo.entrySet()) {
			for (Map<String, String> parameters : entry.getValue()) {
				JTable table = new JTable(new ParameterTableModel(parameters));
				
				// allow sorting
				table.setAutoCreateRowSorter(true);
				
				// sort by parameter name
				table.getRowSorter().toggleSortOrder(0);
				
				addTab(entry.getKey().toString(), new JScrollPane(table));
			}
		}
	}
	

	public static void showMessageDialog(Component parent, File file) {
		final JDialog dialog = new JDialog(TunedUtilities.getWindow(parent), "MediaInfo", ModalityType.DOCUMENT_MODAL);
		dialog.setLocation(TunedUtilities.getPreferredLocation(dialog));
		
		JComponent c = (JComponent) dialog.getContentPane();
		c.setLayout(new MigLayout("fill", "[align center]", "[fill][pref!]"));
		
		MediaInfo mediaInfo = new MediaInfo();
		mediaInfo.open(file);
		
		MediaInfoComponent mediaInfoComponent = new MediaInfoComponent(mediaInfo.snapshot());
		
		mediaInfo.close();
		
		c.add(mediaInfoComponent, "grow, wrap");
		
		c.add(new JButton(new AbstractAction("OK") {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				dialog.setVisible(false);
			}
		}), "wmin 80px, hmin 25px");
		
		dialog.pack();
		dialog.setVisible(true);
	}
	
	
	protected static class ParameterTableModel extends AbstractTableModel {
		
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
