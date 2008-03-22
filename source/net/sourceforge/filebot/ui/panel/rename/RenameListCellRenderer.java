
package net.sourceforge.filebot.ui.panel.rename;


import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListModel;

import net.sourceforge.tuned.ui.DefaultFancyListCellRenderer;


class RenameListCellRenderer extends DefaultFancyListCellRenderer {
	
	private final ListModel names;
	private final ListModel files;
	
	private final JLabel extension = new JLabel(".png");
	
	
	public RenameListCellRenderer(ListModel names, ListModel files) {
		this.names = names;
		this.files = files;
		
		setHighlightingEnabled(false);
		
		this.add(extension);
	}
	
	private final Color noMatchGradientBeginColor = Color.decode("#B7B7B7");
	private final Color noMatchGradientEndColor = Color.decode("#9A9A9A");
	
	
	@Override
	public void configureListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		super.configureListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		
		if (index >= getMinLength()) {
			if (isSelected) {
				setForeground(Color.WHITE);
				setGradientColors(noMatchGradientBeginColor, noMatchGradientEndColor);
			} else {
				setForeground(noMatchGradientBeginColor);
			}
		}
	}
	

	private int getMinLength() {
		if ((names == null) || (files == null))
			return 0;
		
		int n1 = names.getSize();
		int n2 = files.getSize();
		
		return Math.min(n1, n2);
	}
	
}
