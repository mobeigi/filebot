
package net.sourceforge.filebot.ui.panel.sfv;


import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.ui.FileBotPanel;
import net.sourceforge.filebot.ui.FileTransferableMessageHandler;
import net.sourceforge.filebot.ui.SelectDialog;
import net.sourceforge.filebot.ui.transfer.LoadAction;
import net.sourceforge.filebot.ui.transfer.SaveAction;
import net.sourceforge.tuned.FileUtil;
import net.sourceforge.tuned.MessageHandler;
import net.sourceforge.tuned.ui.TunedUtil;


public class SfvPanel extends FileBotPanel {
	
	private final SfvTable sfvTable = new SfvTable();
	
	private final TotalProgressPanel totalProgressPanel = new TotalProgressPanel(sfvTable.getChecksumComputationService());
	
	private final MessageHandler messageHandler = new FileTransferableMessageHandler(this, sfvTable.getTransferablePolicy());
	
	
	public SfvPanel() {
		super("SFV", ResourceManager.getIcon("panel.sfv"));
		
		setBorder(BorderFactory.createTitledBorder("SFV"));
		
		setLayout(new MigLayout("insets 0, nogrid, fill", "", "align bottom"));
		
		add(new JScrollPane(sfvTable), "grow, wrap 10px");
		
		add(new JButton(loadAction), "gap 15px, gap bottom 4px");
		add(new JButton(saveAction), "gap rel, gap bottom 4px");
		add(new JButton(clearAction), "gap rel, gap bottom 4px");
		
		add(totalProgressPanel, "gap left indent:push, gap bottom 2px, gap right 7px, hidemode 3");
		
		// Shortcut DELETE
		TunedUtil.putActionForKeystroke(this, KeyStroke.getKeyStroke("pressed DELETE"), removeAction);
	}
	

	@Override
	public MessageHandler getMessageHandler() {
		return messageHandler;
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
		
		
		public ChecksumTableSaveAction() {
			super(sfvTable.getExportHandler());
		}
		

		@Override
		public ChecksumTableExportHandler getExportHandler() {
			return (ChecksumTableExportHandler) super.getExportHandler();
		}
		

		@Override
		protected boolean canExport() {
			return selectedColumn != null && super.canExport();
		}
		

		@Override
		protected void export(File file) throws IOException {
			getExportHandler().export(file, selectedColumn);
		}
		

		@Override
		protected String getDefaultFileName() {
			return getExportHandler().getDefaultFileName(selectedColumn);
		}
		

		@Override
		protected File getDefaultFolder() {
			// if the column is a folder use it as default folder in the file dialog
			return selectedColumn.isDirectory() ? selectedColumn : null;
		}
		

		@Override
		public void actionPerformed(ActionEvent e) {
			List<File> options = sfvTable.getModel().getChecksumColumns();
			
			this.selectedColumn = null;
			
			if (options.size() == 1) {
				// auto-select option if there is only one option
				this.selectedColumn = options.get(0);
			} else if (options.size() > 1) {
				// user must select one option
				SelectDialog<File> selectDialog = new SelectDialog<File>(SwingUtilities.getWindowAncestor(SfvPanel.this), options) {
					
					@Override
					protected String convertValueToString(Object value) {
						return FileUtil.getFolderName((File) value);
					}
				};
				
				selectDialog.getHeaderLabel().setText("Select checksum column:");
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
