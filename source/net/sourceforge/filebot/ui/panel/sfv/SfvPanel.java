
package net.sourceforge.filebot.ui.panel.sfv;


import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.filebot.ui.FileBotPanel;
import net.sourceforge.filebot.ui.FileTransferableMessageHandler;
import net.sourceforge.filebot.ui.SelectDialog;
import net.sourceforge.filebot.ui.transfer.LoadAction;
import net.sourceforge.filebot.ui.transfer.SaveAction;
import net.sourceforge.tuned.FileUtil;
import net.sourceforge.tuned.MessageBus;
import net.sourceforge.tuned.ui.TunedUtil;


public class SfvPanel extends FileBotPanel {
	
	private SfvTable sfvTable = new SfvTable();
	
	private TotalProgressPanel totalProgressPanel = new TotalProgressPanel(sfvTable.getChecksumComputationService());
	
	
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
		TunedUtil.putActionForKeystroke(this, KeyStroke.getKeyStroke("pressed DELETE"), removeAction);
		
		MessageBus.getDefault().addMessageHandler(getPanelName(), new FileTransferableMessageHandler(this, sfvTable.getTransferablePolicy()));
	}
	
	private final SaveAction saveAction = new ChecksumTableSaveAction();
	
	private final LoadAction loadAction = new LoadAction(sfvTable.getTransferablePolicy());
	
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
	
	
	private class ChecksumTableSaveAction extends SaveAction {
		
		private File selectedColumn = null;
		
		
		@Override
		protected boolean canExport() {
			return selectedColumn != null && sfvTable.getExportHandler().canExport();
		}
		

		@Override
		protected void export(File file) throws IOException {
			sfvTable.getExportHandler().export(file, selectedColumn);
		}
		

		@Override
		protected String getDefaultFileName() {
			return sfvTable.getExportHandler().getDefaultFileName(selectedColumn);
		}
		

		@Override
		protected File getDefaultFolder() {
			// if column is a folder use it as default folder in file dialog
			return selectedColumn.isDirectory() ? selectedColumn : null;
		}
		

		@Override
		public void actionPerformed(ActionEvent e) {
			List<File> options = sfvTable.getModel().getChecksumColumns();
			
			this.selectedColumn = null;
			
			if (options.size() == 1) {
				// auto-select if there is only one option
				this.selectedColumn = options.get(0);
			} else if (options.size() > 1) {
				// show user his/her options
				SelectDialog<File> selectDialog = new SelectDialog<File>(SwingUtilities.getWindowAncestor(SfvPanel.this), options) {
					
					@Override
					protected String convertValueToString(Object value) {
						return FileUtil.getFolderName((File) value);
					}
				};
				
				selectDialog.setText("Select checksum column:");
				selectDialog.setVisible(true);
				
				this.selectedColumn = selectDialog.getSelectedValue();
			}
			
			if (this.selectedColumn != null) {
				// continue if a column was selected
				super.actionPerformed(e);
			}
		}
		
	}
	
}
