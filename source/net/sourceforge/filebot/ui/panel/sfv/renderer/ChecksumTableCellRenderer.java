
package net.sourceforge.filebot.ui.panel.sfv.renderer;


import java.awt.Component;

import javax.swing.JTable;

import net.sourceforge.filebot.ui.panel.sfv.Checksum;


public class ChecksumTableCellRenderer extends TextTableCellRenderer {
	
	private ProgressBarTableCellRenderer progressBarRenderer = new ProgressBarTableCellRenderer();
	
	
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		super.getTableCellRendererComponent(table, null, isSelected, hasFocus, row, column);
		
		if (value == null)
			return this;
		
		Checksum checksum = (Checksum) value;
		
		if (checksum.getState() == Checksum.State.READY) {
			setText(checksum.getChecksumString());
		} else if (checksum.getState() == Checksum.State.PENDING) {
			setText("Pending ...");
		} else {
			return progressBarRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		}
		
		return this;
	}
}
