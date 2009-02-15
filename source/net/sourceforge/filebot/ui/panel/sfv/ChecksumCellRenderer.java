
package net.sourceforge.filebot.ui.panel.sfv;


import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.SwingWorker.StateValue;
import javax.swing.table.DefaultTableCellRenderer;

import net.sourceforge.tuned.ExceptionUtilities;


public class ChecksumCellRenderer extends DefaultTableCellRenderer {
	
	private final SwingWorkerCellRenderer progressRenderer = new SwingWorkerCellRenderer();
	
	
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		boolean pendingWorker = false;
		
		if (value instanceof SwingWorker) {
			if (((SwingWorker<?, ?>) value).getState() != StateValue.PENDING)
				return progressRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			
			pendingWorker = true;
		}
		
		// ignore focus
		super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
		
		// restore text color
		setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
		setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
		
		if (pendingWorker) {
			setText("Pending...");
		} else if (value == null && !isSelected) {
			setBackground(derive(table.getGridColor(), 0.1f));
		} else if (value instanceof Throwable) {
			setText(ExceptionUtilities.getRootCauseMessage((Throwable) value));
			
			if (!isSelected) {
				setForeground(Color.RED);
			}
		}
		
		return this;
	}
	

	private Color derive(Color color, float alpha) {
		return new Color(((((int) (alpha * 255)) & 0xFF) << 24) & (color.getRGB() | 0xFF000000), true);
	}
}
