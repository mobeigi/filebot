
package net.sourceforge.filebot.ui.panel.sfv;


import java.awt.dnd.DnDConstants;
import java.awt.event.MouseEvent;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.MouseInputListener;
import javax.swing.event.TableModelEvent;
import javax.swing.plaf.basic.BasicTableUI;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import net.sourceforge.filebot.FileBotUtilities;
import net.sourceforge.filebot.ui.transfer.DefaultTransferHandler;


class SfvTable extends JTable {
	
	private final SfvTransferablePolicy transferablePolicy;
	private final ChecksumTableExportHandler exportHandler;
	
	private final ChecksumComputationService checksumComputationService = new ChecksumComputationService();
	
	
	public SfvTable() {
		transferablePolicy = new SfvTransferablePolicy(getModel(), checksumComputationService);
		exportHandler = new ChecksumTableExportHandler(getModel());
		
		setFillsViewportHeight(true);
		setAutoCreateRowSorter(true);
		setAutoCreateColumnsFromModel(true);
		setAutoResizeMode(AUTO_RESIZE_SUBSEQUENT_COLUMNS);
		
		setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		
		setRowHeight(20);
		
		setTransferHandler(new DefaultTransferHandler(transferablePolicy, exportHandler));
		setDragEnabled(true);
		
		setUI(new DragDropRowTableUI());
		
		// highlight CRC32 patterns in filenames in green and with smaller font-size
		setDefaultRenderer(String.class, new HighlightPatternCellRenderer(FileBotUtilities.EMBEDDED_CHECKSUM_PATTERN, "#009900", "smaller"));
		setDefaultRenderer(ChecksumRow.State.class, new StateIconTableCellRenderer());
		setDefaultRenderer(ChecksumCell.class, new ChecksumTableCellRenderer());
	}
	

	public SfvTransferablePolicy getTransferablePolicy() {
		return transferablePolicy;
	}
	

	public ChecksumTableExportHandler getExportHandler() {
		return exportHandler;
	}
	

	public ChecksumComputationService getChecksumComputationService() {
		return checksumComputationService;
	}
	

	@Override
	public DefaultTransferHandler getTransferHandler() {
		return (DefaultTransferHandler) super.getTransferHandler();
	}
	

	@Override
	protected TableModel createDefaultDataModel() {
		return new ChecksumTableModel();
	}
	

	@Override
	public ChecksumTableModel getModel() {
		return (ChecksumTableModel) super.getModel();
	}
	

	@Override
	public void createDefaultColumnsFromModel() {
		super.createDefaultColumnsFromModel();
		
		for (int i = 0; i < getColumnCount(); i++) {
			TableColumn column = getColumnModel().getColumn(i);
			
			if (i == 0) {
				column.setPreferredWidth(45);
			} else if (i == 1) {
				column.setPreferredWidth(400);
			} else if (i >= 2) {
				column.setPreferredWidth(150);
			}
		}
	}
	

	public void clear() {
		checksumComputationService.reset();
		transferablePolicy.reset();
		
		getModel().clear();
	}
	

	@Override
	public void tableChanged(TableModelEvent e) {
		//TODO CCS in SfvPanel??
		if (e.getType() == TableModelEvent.DELETE) {
			// remove cancelled tasks from queue
			checksumComputationService.purge();
		}
		
		super.tableChanged(e);
	}
	
	
	/**
	 * When trying to drag a row of a multi-select JTable, it will start selecting rows instead
	 * of initiating a drag. This TableUI will give the JTable proper dnd behaviour.
	 */
	private class DragDropRowTableUI extends BasicTableUI {
		
		@Override
		protected MouseInputListener createMouseInputListener() {
			return new DragDropRowMouseInputHandler();
		}
		
		
		private class DragDropRowMouseInputHandler extends MouseInputHandler {
			
			@Override
			public void mouseDragged(MouseEvent e) {
				// Only do special handling if we are drag enabled with multiple selection
				if (table.getDragEnabled() && table.getSelectionModel().getSelectionMode() == ListSelectionModel.MULTIPLE_INTERVAL_SELECTION) {
					table.getTransferHandler().exportAsDrag(table, e, DnDConstants.ACTION_COPY);
				} else {
					super.mouseDragged(e);
				}
			}
		}
	}
	
}
