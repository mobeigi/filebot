
package net.sourceforge.filebot.ui.panel.sfv;


import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import net.miginfocom.swing.MigLayout;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.ui.FileBotPanel;
import net.sourceforge.filebot.ui.FileTransferableMessageHandler;
import net.sourceforge.filebot.ui.SelectDialog;
import net.sourceforge.filebot.ui.transfer.DefaultTransferHandler;
import net.sourceforge.filebot.ui.transfer.LoadAction;
import net.sourceforge.filebot.ui.transfer.SaveAction;
import net.sourceforge.tuned.FileUtilities;
import net.sourceforge.tuned.MessageHandler;
import net.sourceforge.tuned.ui.TunedUtilities;


public class SfvPanel extends FileBotPanel {
	
	private final ChecksumComputationService computationService = new ChecksumComputationService();
	
	private final SfvTable table = new SfvTable();
	
	private final SfvTransferablePolicy transferablePolicy = new SfvTransferablePolicy(table.getModel(), computationService);
	private final ChecksumTableExportHandler exportHandler = new ChecksumTableExportHandler(table.getModel());
	
	private final MessageHandler messageHandler = new FileTransferableMessageHandler(this, transferablePolicy);
	
	
	public SfvPanel() {
		super("SFV", ResourceManager.getIcon("panel.sfv"));
		
		table.setTransferHandler(new DefaultTransferHandler(transferablePolicy, exportHandler));
		table.setDragEnabled(true);
		
		JPanel contentPane = new JPanel(new MigLayout("insets 0, nogrid, fill", null, "align bottom"));
		contentPane.setBorder(new TitledBorder(getPanelName()));
		
		setLayout(new MigLayout("insets dialog, fill"));
		add(contentPane, "grow");
		
		contentPane.add(new JScrollPane(table), "grow, wrap 10px");
		
		contentPane.add(new JButton(loadAction), "gap 15px, gap bottom 4px");
		contentPane.add(new JButton(saveAction), "gap rel, gap bottom 4px");
		contentPane.add(new JButton(clearAction), "gap rel, gap bottom 4px");
		
		contentPane.add(new TotalProgressPanel(computationService), "gap left indent:push, gap bottom 2px, gap right 7px, hidemode 3");
		
		// Shortcut DELETE
		TunedUtilities.putActionForKeystroke(this, KeyStroke.getKeyStroke("pressed DELETE"), removeAction);
	}
	

	@Override
	public MessageHandler getMessageHandler() {
		return messageHandler;
	}
	
	private final SaveAction saveAction = new ChecksumTableSaveAction();
	
	private final LoadAction loadAction = new LoadAction(transferablePolicy);
	
	private final AbstractAction clearAction = new AbstractAction("Clear", ResourceManager.getIcon("action.clear")) {
		
		public void actionPerformed(ActionEvent e) {
			transferablePolicy.reset();
			computationService.reset();
			
			table.getModel().clear();
		}
	};
	
	private final AbstractAction removeAction = new AbstractAction("Remove") {
		
		public void actionPerformed(ActionEvent e) {
			if (table.getSelectedRowCount() < 1)
				return;
			
			int firstSelectedRow = table.getSelectedRow();
			
			// remove selected rows
			table.getModel().remove(table.getSelectedRows());
			
			// update computation service task count
			computationService.purge();
			
			// auto select next row
			firstSelectedRow = Math.min(firstSelectedRow, table.getRowCount() - 1);
			
			table.getSelectionModel().setSelectionInterval(firstSelectedRow, firstSelectedRow);
		}
	};
	
	
	protected class ChecksumTableSaveAction extends SaveAction {
		
		private File selectedColumn = null;
		
		
		public ChecksumTableSaveAction() {
			super(exportHandler);
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
			List<File> options = table.getModel().getChecksumColumns();
			
			this.selectedColumn = null;
			
			if (options.size() == 1) {
				// auto-select option if there is only one option
				this.selectedColumn = options.get(0);
			} else if (options.size() > 1) {
				// user must select one option
				SelectDialog<File> selectDialog = new SelectDialog<File>(SwingUtilities.getWindowAncestor(SfvPanel.this), options) {
					
					@Override
					protected String convertValueToString(Object value) {
						return FileUtilities.getFolderName((File) value);
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
