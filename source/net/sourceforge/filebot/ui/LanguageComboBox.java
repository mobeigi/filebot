
package net.sourceforge.filebot.ui;


import static java.awt.event.ItemEvent.*;
import static net.sourceforge.filebot.ui.Language.*;
import static net.sourceforge.filebot.ui.LanguageComboBoxModel.*;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.AbstractList;
import java.util.Locale;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import net.sourceforge.filebot.Settings;
import net.sourceforge.tuned.PreferencesList;
import net.sourceforge.tuned.PreferencesMap.PreferencesEntry;


public class LanguageComboBox extends JComboBox {
	
	private final PreferencesEntry<String> persistentSelectedLanguage;
	private final PreferencesList<String> persistentFavoriteLanguages;
	

	public LanguageComboBox(JComponent parent, Language initialSelection) {
		super(new LanguageComboBoxModel(initialSelection != ALL_LANGUAGES, initialSelection));
		setRenderer(new LanguageComboBoxCellRenderer(super.getRenderer()));
		
		persistentSelectedLanguage = Settings.forPackage(parent.getClass()).entry("language.selected");
		persistentFavoriteLanguages = Settings.forPackage(parent.getClass()).node("language.favorites").asList();
		
		// restore selected language
		getModel().setSelectedItem(Language.getLanguage(persistentSelectedLanguage.getValue()));
		
		// restore favorite languages
		for (String favoriteLanguage : persistentFavoriteLanguages) {
			getModel().favorites().add(getModel().favorites().size(), getLanguage(favoriteLanguage));
		}
		
		// guess favorite languages
		if (getModel().favorites().isEmpty()) {
			for (Locale locale : new Locale[] { Locale.getDefault(), Locale.ENGLISH }) {
				getModel().favorites().add(getLanguage(locale.getLanguage()));
			}
		}
		
		// update favorites on change
		addPopupMenuListener(new PopupSelectionListener() {
			
			@Override
			public void itemStateChanged(ItemEvent e) {
				Language language = (Language) e.getItem();
				
				if (getModel().favorites().add(language)) {
					persistentFavoriteLanguages.set(new AbstractList<String>() {
						
						@Override
						public String get(int index) {
							return getModel().favorites().get(index).getCode();
						}
						

						@Override
						public int size() {
							return getModel().favorites().size();
						}
					});
				}
				
				persistentSelectedLanguage.setValue(language.getCode());
			}
		});
	}
	

	@Override
	public LanguageComboBoxModel getModel() {
		return (LanguageComboBoxModel) super.getModel();
	}
	

	private static class PopupSelectionListener implements PopupMenuListener, ItemListener {
		
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
	
}
