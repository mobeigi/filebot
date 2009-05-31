
package net.sourceforge.filebot.ui.panel.subtitle;


import java.awt.Color;
import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import net.sourceforge.filebot.ResourceManager;


class LanguageComboBoxCellRenderer extends DefaultListCellRenderer {
	
	private final Border padding = new EmptyBorder(2, 2, 2, 2);
	
	private final Border favoritePadding = new EmptyBorder(0, 6, 0, 6);
	
	
	@Override
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		
		Language language = (Language) value;
		setText(language.getName());
		setIcon(ResourceManager.getFlagIcon(language.getCode()));
		
		// default padding
		setBorder(padding);
		
		LanguageComboBoxModel model = (LanguageComboBoxModel) list.getModel();
		
		if ((index > 0 && index <= model.favorites().size())) {
			// add favorite padding
			setBorder(new CompoundBorder(favoritePadding, getBorder()));
		}
		
		if (index == 0 || index == model.favorites().size()) {
			// add separator border
			setBorder(new CompoundBorder(new DashedSeparator(10, 4, Color.lightGray, list.getBackground()), getBorder()));
		}
		
		return this;
	}
	
}
