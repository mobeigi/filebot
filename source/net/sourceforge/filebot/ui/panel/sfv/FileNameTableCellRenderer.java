
package net.sourceforge.filebot.ui.panel.sfv;


import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;


/**
 * DefaultTableCellRenderer that will highlight CRC32 patterns.
 */
class FileNameTableCellRenderer extends DefaultTableCellRenderer {
	
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		
		super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
		
		if (hasFocus) {
			// do not highlight checksum patterns in focused cells, that would look weird 
			return this;
		}
		
		// change font-color of checksum pattern
		String htmlText = value.toString().replaceAll("\\[(\\p{XDigit}{8})\\]", "[<font color='#009900'>$1</font>]");
		
		// use no-break, because we really don't want line-wrapping in our table cells
		setText("<html><nobr>" + htmlText + "</nobr></html>");
		
		return this;
	}
}
