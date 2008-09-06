
package net.sourceforge.filebot.ui.panel.sfv.renderer;


import java.awt.Component;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;


public class FileNameTableCellRenderer extends DefaultTableCellRenderer {
	
	private final Pattern checksumPattern = Pattern.compile("\\[(\\p{XDigit}{8})\\]");
	
	
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		
		super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		
		if (hasFocus) {
			// do not highlight text is focused, that just looks weird
			return this;
		}
		
		String text = value.toString();
		Matcher matcher = checksumPattern.matcher(text);
		
		if (!matcher.find()) {
			// break if name does not contain a checksum that needs to be highlighted
			return this;
		}
		
		matcher.reset();
		StringBuffer sb = new StringBuffer("<html>");
		
		while (matcher.find()) {
			matcher.appendReplacement(sb, "[<font color=#009900>$1</font>]");
		}
		
		matcher.appendTail(sb);
		sb.append("</html>");
		
		setText(sb.toString());
		
		return this;
	}
}
