
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
	
	
	public HighlightPatternCellRenderer(Pattern pattern) {
		this.pattern = pattern;
	}
	

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
		
		boolean isWarning = (table.getValueAt(row, 0) == ChecksumRow.State.WARNING);
		
		// highlight CRC32 checksum patterns by using a smaller font-size and changing the font-color to a dark green
		// do not change the font-color if cell is selected, because that would look ugly (imagine green text on blue background ...)
		Matcher matcher = pattern.matcher(value.toString());
		
		// use no-break, because we really don't want line-wrapping in our table cells
		StringBuffer htmlText = new StringBuffer("<html><nobr>");
		
		while (matcher.find()) {
			matcher.appendReplacement(htmlText, createReplacement(isSelected ? null : isWarning ? "#FF8C00" : "#009900", "smaller"));
		}
		
		matcher.appendTail(htmlText);
		
		htmlText.append("</nobr></html>");
		
		setText(htmlText.toString());
		
		return this;
	}
	

	protected String createReplacement(String cssColor, String cssFontSize) {
		// build replacement string like
		// e.g. <span style='font-size: smaller; color: #009900;'>$0</span>
		StringBuilder replacement = new StringBuilder(60);
		
		replacement.append("<span style='");
		
		if (cssColor != null) {
			replacement.append("color:").append(cssColor).append(';');
		}
		
		if (cssFontSize != null) {
			replacement.append("font-size:").append(cssFontSize).append(';');
		}
		
		return replacement.append("'>$0</span>").toString();
	}
	
}
