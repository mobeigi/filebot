
package net.sourceforge.filebot.ui.panel.sfv;


import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;


/**
 * DefaultTableCellRenderer with highlighting of text patterns.
 */
class HighlightPatternCellRenderer extends DefaultTableCellRenderer {
	
	private final String pattern;
	private final String cssColor;
	private final String cssFontSize;
	
	
	public HighlightPatternCellRenderer(String pattern, String cssColor, String cssFontSize) {
		this.pattern = pattern;
		this.cssColor = cssColor;
		this.cssFontSize = cssFontSize;
	}
	

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
		
		// highlight CRC32 checksum patterns by using a smaller font-size and changing the font-color to a dark green
		// do not change the font-color if cell is selected, because that would look ugly (imagine green text on blue background ...)
		String htmlText = value.toString().replaceAll(pattern, "[<span style='font-size: " + cssFontSize + ";" + (!isSelected ? "color: " + cssColor + ";" : "") + "'>$1</span>]");
		
		// use no-break, because we really don't want line-wrapping in our table cells
		setText("<html><nobr>" + htmlText + "</nobr></html>");
		
		return this;
	}
}
