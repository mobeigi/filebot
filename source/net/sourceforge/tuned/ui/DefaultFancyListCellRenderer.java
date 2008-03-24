
package net.sourceforge.tuned.ui;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Insets;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;


public class DefaultFancyListCellRenderer extends AbstractFancyListCellRenderer {
	
	private final JLabel label = new JLabel();
	
	
	public DefaultFancyListCellRenderer() {
		add(label, BorderLayout.WEST);
	}
	

	public DefaultFancyListCellRenderer(int padding) {
		super(new Insets(padding, padding, padding, padding));
		add(label, BorderLayout.WEST);
	}
	

	protected DefaultFancyListCellRenderer(Object constraint, int padding, int margin, Color selectedBorderColor) {
		super(new Insets(padding, padding, padding, padding), new Insets(margin, margin, margin, margin), selectedBorderColor);
		add(label, constraint);
	}
	

	@Override
	protected void configureListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		super.configureListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		
		label.setOpaque(false);
		
		setText(value.toString());
	}
	

	public void setIcon(Icon icon) {
		label.setIcon(icon);
	}
	

	public void setText(String text) {
		label.setText(text);
	}
	

	public void setHorizontalTextPosition(int textPosition) {
		label.setHorizontalTextPosition(textPosition);
	}
	

	public void setVerticalTextPosition(int textPosition) {
		label.setVerticalTextPosition(textPosition);
	}
	

	@Override
	public void setForeground(Color fg) {
		super.setForeground(fg);
		
		// label is null while in super constructor
		if (label != null) {
			label.setForeground(fg);
		}
	}
	
}
