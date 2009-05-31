
package net.sourceforge.filebot.ui.panel.subtitle;


import static java.awt.event.ItemEvent.*;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JComboBox;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;


class PopupSelectionListener implements PopupMenuListener, ItemListener {
	
	private Object selected = null;
	
	
	@Override
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
		JComboBox comboBox = (JComboBox) e.getSource();
		
		// selected item before popup
		selected = comboBox.getSelectedItem();
	}
	

	@Override
	public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
		JComboBox comboBox = (JComboBox) e.getSource();
		
		// check selected item after popup
		if (selected != comboBox.getSelectedItem()) {
			itemStateChanged(new ItemEvent(comboBox, ITEM_STATE_CHANGED, comboBox.getSelectedItem(), SELECTED));
		}
		
		selected = null;
	}
	

	@Override
	public void popupMenuCanceled(PopupMenuEvent e) {
		selected = null;
	}
	

	@Override
	public void itemStateChanged(ItemEvent e) {
		
	}
}
