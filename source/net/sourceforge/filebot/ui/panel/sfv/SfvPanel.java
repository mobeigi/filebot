
package net.sourceforge.filebot.ui.panel.sfv;


import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import net.sourceforge.filebot.FileBotUtil;
import net.sourceforge.filebot.FileFormat;
import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.filebot.ui.FileBotPanel;
import net.sourceforge.filebot.ui.FileTransferableMessageHandler;
import net.sourceforge.filebot.ui.SelectDialog;
import net.sourceforge.filebot.ui.transfer.LoadAction;
import net.sourceforge.filebot.ui.transfer.SaveAction;
import net.sourceforge.tuned.MessageBus;


public class SfvPanel extends FileBotPanel {
	
	private SfvTable sfvTable = new SfvTable();
	
	private TotalProgressPanel totalProgressPanel = new TotalProgressPanel();
	
	
	public SfvPanel() {
		super("SFV", ResourceManager.getIcon("panel.sfv"));
		
		setBorder(BorderFactory.createTitledBorder("SFV"));
		
		JPanel southPanel = new JPanel(new BorderLayout());
		
		JPanel southEastPanel = new JPanel(new BorderLayout());
		
		Box buttonBox = Box.createHorizontalBox();
		buttonBox.setBorder(new EmptyBorder(5, 15, 5, 15));
		
		buttonBox.add(new JButton(loadAction));
		buttonBox.add(Box.createHorizontalStrut(5));
		buttonBox.add(new JButton(saveAction));
		buttonBox.add(Box.createHorizontalStrut(5));
		buttonBox.add(new JButton(clearAction));
		southEastPanel.add(buttonBox, BorderLayout.SOUTH);
		
		southPanel.add(southEastPanel, BorderLayout.WEST);
		southPanel.add(totalProgressPanel, BorderLayout.EAST);
		
		add(new JScrollPane(sfvTable), BorderLayout.CENTER);
		add(southPanel, BorderLayout.SOUTH);
		
		// Shortcut DELETE
		FileBotUtil.registerActionForKeystroke(this, KeyStroke.getKeyStroke("pressed DELETE"), removeAction);
		
		MessageBus.getDefault().addMessageHandler(getPanelName(), new FileTransferableMessageHandler(getPanelName(), sfvTable));
	}
	
	private final SaveAction saveAction = new SaveAction(sfvTable) {
		
		private int index;
		private String name;
		
		private File folder = null;
		
		
		@Override
		protected void save(File file) {
			sfvTable.save(file, index);
		}
		

		@Override
		protected String getDefaultFileName() {
			return name;
		}
		

		@Override
		protected File getDefaultFolder() {
			return folder;
		}
		

		@Override
		public void actionPerformed(ActionEvent e) {
			ChecksumTableModel model = (ChecksumTableModel) sfvTable.getModel();
			
			ArrayList<File> options = new ArrayList<File>();
			
			for (int i = 0; i < model.getChecksumColumnCount(); i++) {
				options.add(model.getChecksumColumnRoot(i));
			}
			
			File selected = null;
			
			if (options.size() > 1) {
				SelectDialog<File> selectDialog = new SelectDialog<File>(SwingUtilities.getWindowAncestor(SfvPanel.this), options) {
					
					@Override
					protected String convertValueToString(Object value) {
						File columnRoot = (File) value;
						return FileFormat.getName(columnRoot);
					}
				};
				
				selectDialog.setText("Select checksum column:");
				selectDialog.setVisible(true);
				selected = selectDialog.getSelectedValue();
			} else if (options.size() == 1) {
				selected = options.get(0);
			}
			
			if (selected == null)
				return;
			
			index = options.indexOf(selected);
			name = FileFormat.getNameWithoutExtension(selected);
			
			if (name.isEmpty())
				name = "name";
			
			name += ".sfv";
			
			// selected is either a folder or a sfv file
			if (selected.isDirectory()) {
				folder = selected;
			}
			
			super.actionPerformed(e);
		}
	};
	
	private final LoadAction loadAction = new LoadAction(sfvTable);
	
	private final AbstractAction clearAction = new AbstractAction("Clear", ResourceManager.getIcon("action.clear")) {
		
		public void actionPerformed(ActionEvent e) {
			sfvTable.clear();
		}
	};
	
	private final AbstractAction removeAction = new AbstractAction("Remove") {
		
		public void actionPerformed(ActionEvent e) {
			if (sfvTable.getSelectedRowCount() < 1)
				return;
			
			int row = sfvTable.getSelectionModel().getMinSelectionIndex();
			
			sfvTable.removeRows(sfvTable.getSelectedRows());
			
			int maxRow = sfvTable.getRowCount() - 1;
			
			if (row > maxRow)
				row = maxRow;
			
			sfvTable.getSelectionModel().setSelectionInterval(row, row);
		}
	};
	
}
