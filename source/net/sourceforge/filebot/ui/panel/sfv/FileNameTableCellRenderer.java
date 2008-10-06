
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
		
		// highlight CRC32 checksum patterns by using a smaller font-size and changing the font-color to a dark green
		// do not change the font-color if cell is selected, because that would look ugly (imagine green text on blue background ...)
		String htmlText = value.toString().replaceAll("\\[(\\p{XDigit}{8})\\]", "[<span style='font-size: smaller;" + (!isSelected ? "color: #009900;" : "") + "'>$1</span>]");
		
		// use no-break, because we really don't want line-wrapping in our table cells
		setText("<html><nobr>" + htmlText + "</nobr></html>");
		
		return this;
	}
}
