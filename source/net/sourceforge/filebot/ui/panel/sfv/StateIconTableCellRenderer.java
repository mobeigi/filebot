
package net.sourceforge.filebot.ui.panel.sfv;


import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

import net.sourceforge.filebot.resources.ResourceManager;


class StateIconTableCellRenderer extends DefaultTableCellRenderer {
	
	private Icon warning = ResourceManager.getIcon("status.warning");
	private Icon error = ResourceManager.getIcon("status.error");
	private Icon unknown = ResourceManager.getIcon("status.unknown");
	private Icon ok = ResourceManager.getIcon("status.ok");
	
	
	public StateIconTableCellRenderer() {
		setVerticalAlignment(SwingConstants.CENTER);
		setHorizontalAlignment(SwingConstants.CENTER);
	}
	

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		super.getTableCellRendererComponent(table, null, isSelected, false, row, column);
		
		ChecksumRow.State state = (ChecksumRow.State) value;
		
		switch (state) {
			case OK:
				setIcon(ok);
				break;
			case ERROR:
				setIcon(error);
				break;
			case WARNING:
				setIcon(warning);
				break;
			case UNKNOWN:
				setIcon(unknown);
				break;
		}
		
		return this;
	}
	
}
