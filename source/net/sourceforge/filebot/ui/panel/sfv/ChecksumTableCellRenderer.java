
package net.sourceforge.filebot.ui.panel.sfv;


import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Rectangle;

import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import net.sourceforge.tuned.ExceptionUtilities;


class ChecksumTableCellRenderer extends DefaultTableCellRenderer {
	
	private final ProgressBarTableCellRenderer progressBarRenderer = new ProgressBarTableCellRenderer();
	
	
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		super.getTableCellRendererComponent(table, null, isSelected, false, row, column);
		
		if (value instanceof ChecksumCell) {
			ChecksumCell checksum = (ChecksumCell) value;
			
			switch (checksum.getState()) {
				case READY:
					setText(checksum.getChecksum(HashType.CRC32));
					break;
				case PENDING:
					setText("Pending ...");
					break;
				case ERROR:
					setText(ExceptionUtilities.getMessage(checksum.getError()));
					break;
				default:
					return progressBarRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			}
		}
		
		return this;
	}
	
	
	private static class ProgressBarTableCellRenderer extends JPanel implements TableCellRenderer {
		
		private final JProgressBar progressBar = new JProgressBar(0, 100);
		
		
		public ProgressBarTableCellRenderer() {
			progressBar.setStringPainted(true);
			
			setLayout(new BorderLayout());
			add(progressBar, BorderLayout.CENTER);
			
			setBorder(new EmptyBorder(2, 2, 2, 2));
		}
		

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			
			ChecksumComputationTask task = ((ChecksumCell) value).getTask();
			
			if (task != null) {
				progressBar.setValue(task.getProgress());
			}
			
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
		@Override
		public void repaint(long tm, int x, int y, int width, int height) {
		}
		

		/**
		 * Overridden for performance reasons.
		 */
		@Override
		public void repaint(Rectangle r) {
		}
		

		/**
		 * Overridden for performance reasons.
		 */
		@Override
		public void repaint() {
		}
		

		/**
		 * Overridden for performance reasons.
		 */
		@Override
		public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
		}
		
	}
}
