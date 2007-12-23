
package net.sourceforge.filebot.ui.panel.rename;


import java.awt.Color;
import java.awt.Component;

import javax.swing.JList;
import javax.swing.ListModel;

import net.sourceforge.tuned.ui.FancyListCellRenderer;


class RenameListCellRenderer extends FancyListCellRenderer {
	
	private ListModel names;
	private ListModel files;
	
	
	public RenameListCellRenderer(ListModel names, ListModel files) {
		this.names = names;
		this.files = files;
	}
	
	private Color noMatchGradientBeginColor = Color.decode("#B7B7B7");
	private Color noMatchGradientEndColor = Color.decode("#9A9A9A");
	
	
	@Override
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		
		if (index >= getMinLength())
			if (isSelected) {
				setForeground(Color.WHITE);
				setGradientBeginColor(noMatchGradientBeginColor);
				setGradientEndColor(noMatchGradientEndColor);
			} else {
				setForeground(noMatchGradientBeginColor);
			}
		
		return this;
	}
	

	private int getMinLength() {
		if (names == null || files == null)
			return 0;
		
		int n1 = names.getSize();
		int n2 = files.getSize();
		
		return Math.min(n1, n2);
	}
	
}
