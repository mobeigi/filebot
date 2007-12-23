
package net.sourceforge.filebot.ui.panel.sfv.renderer;


import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import net.sourceforge.filebot.ui.panel.sfv.Checksum;


public class ProgressBarTableCellRenderer extends JPanel implements TableCellRenderer {
	
	private JProgressBar progressBar = new JProgressBar(0, 100);
	
	
	public ProgressBarTableCellRenderer() {
		super(new BorderLayout());
		
		progressBar.setStringPainted(true);
		add(progressBar, BorderLayout.CENTER);
		
		int margin = 2;
		setBorder(BorderFactory.createEmptyBorder(margin, margin, margin, margin));
	}
	

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		Checksum checksum = (Checksum) value;
		
		Integer progress = checksum.getProgress();
		progressBar.setValue(progress);
		
		if (isSelected) {
			this.setBackground(table.getSelectionBackground());
		} else {
			this.setBackground(table.getBackground());
		}
		
		return this;
	}
	

	/**
	 * Overridden for performance reasons.
	 */
	public void repaint(long tm, int x, int y, int width, int height) {
	}
	

	/**
	 * Overridden for performance reasons.
	 */
	public void repaint(Rectangle r) {
	}
	

	/**
	 * Overridden for performance reasons.
	 */
	public void repaint() {
	}
	

	/**
	 * Overridden for performance reasons.
	 */
	public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
	}
	
}
