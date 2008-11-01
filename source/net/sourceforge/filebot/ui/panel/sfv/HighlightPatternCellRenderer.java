
package net.sourceforge.filebot.ui.panel.sfv;


import java.awt.Component;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;


/**
 * DefaultTableCellRenderer with highlighting of text patterns.
 */
class HighlightPatternCellRenderer extends DefaultTableCellRenderer {
	
	private final Pattern pattern;
	private final String cssColor;
	private final String cssFontSize;
	
	
	public HighlightPatternCellRenderer(Pattern pattern, String cssColor, String cssFontSize) {
		this.pattern = pattern;
		this.cssColor = cssColor;
		this.cssFontSize = cssFontSize;
	}
	

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
		
		// highlight CRC32 checksum patterns by using a smaller font-size and changing the font-color to a dark green
		// do not change the font-color if cell is selected, because that would look ugly (imagine green text on blue background ...)
		Matcher matcher = pattern.matcher(value.toString());
		
		// use no-break, because we really don't want line-wrapping in our table cells
		StringBuffer htmlText = new StringBuffer("<html><nobr>");
		
		while (matcher.find()) {
			matcher.appendReplacement(htmlText, "<span style='font-size: " + cssFontSize + ";" + (!isSelected ? "color: " + cssColor + ";" : "") + "'>$1</span>");
		}
		
		matcher.appendTail(htmlText);
		
		htmlText.append("</nobr></html>");
		
		setText(htmlText.toString());
		
		return this;
	}
}
