
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
			matcher.appendReplacement(htmlText, createReplacement(isSelected));
		}
		
		matcher.appendTail(htmlText);
		
		htmlText.append("</nobr></html>");
		
		setText(htmlText.toString());
		
		return this;
	}
	

	protected String createReplacement(boolean isSelected) {
		// build replacement string like
		// e.g. <span style='font-size: smaller; color: #009900;'>$0</span>
		StringBuilder replacement = new StringBuilder(60);
		
		replacement.append("<span style='");
		replacement.append("font-size:").append(cssFontSize).append(';');
		
		if (!isSelected) {
			replacement.append("color:").append(cssColor).append(';');
		}
		
		return replacement.append("'>$0</span>").toString();
	}
	
}
