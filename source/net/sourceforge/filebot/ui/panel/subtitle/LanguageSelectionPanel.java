
package net.sourceforge.filebot.ui.panel.subtitle;


import java.awt.AlphaComposite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JPanel;
import javax.swing.JToggleButton;

import net.sourceforge.filebot.Settings;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FunctionList;
import ca.odell.glazedlists.ListSelection;
import ca.odell.glazedlists.UniqueList;
import ca.odell.glazedlists.FunctionList.Function;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;


public class LanguageSelectionPanel extends JPanel {
	
	private final ListSelection<Language> selectionModel;
	
	private final Map<String, Boolean> defaultSelection = new TreeMap<String, Boolean>(String.CASE_INSENSITIVE_ORDER);
	private final Map<String, Boolean> globalSelection = Settings.getSettings().asBooleanMap(Settings.SUBTITLE_LANGUAGE);
	
	
	public LanguageSelectionPanel(EventList<SubtitlePackage> source) {
		super(new FlowLayout(FlowLayout.RIGHT, 5, 1));
		
		defaultSelection.putAll(globalSelection);
		
		EventList<Language> languageList = new FunctionList<SubtitlePackage, Language>(source, new LanguageFunction());
		EventList<Language> languageSet = new UniqueList<Language>(languageList);
		
		selectionModel = new ListSelection<Language>(languageSet);
		
		selectionModel.getSource().addListEventListener(new SourceChangeHandler());
	}
	

	public EventList<Language> getSelected() {
		return selectionModel.getSelected();
	}
	

	private boolean isSelectedByDefault(Language language) {
		Boolean selected = defaultSelection.get(language.getName());
		
		if (selected != null)
			return selected;
		
		// deselected by default
		return false;
	}
	

	private void setSelected(Language language, boolean selected) {
		String key = language.getName();
		
		defaultSelection.put(key, selected);
		globalSelection.put(key, selected);
		
		if (selected)
			selectionModel.select(language);
		else
			selectionModel.deselect(selectionModel.getSource().indexOf(language));
	}
	
	
	/**
	 * Provide the binding between this panel and the source {@link EventList}.
	 */
	private class SourceChangeHandler implements ListEventListener<Language> {
		
		/**
		 * Handle an inserted element.
		 */
		private void insert(int index) {
			Language language = selectionModel.getSource().get(index);
			
			LanguageToggleButton button = new LanguageToggleButton(language);
			button.setSelected(isSelectedByDefault(language));
			
			add(button, index);
		}
		

		/**
		 * Handle a deleted element.
		 */
		private void delete(int index) {
			remove(index);
		}
		

		/**
		 * When the components list changes, this updates the panel.
		 */
		public void listChanged(ListEvent<Language> listChanges) {
			while (listChanges.next()) {
				int type = listChanges.getType();
				int index = listChanges.getIndex();
				
				if (type == ListEvent.INSERT) {
					insert(index);
				} else if (type == ListEvent.DELETE) {
					delete(index);
				}
			}
			
			// repaint the panel
			revalidate();
			repaint();
		}
	}
	

	private class LanguageToggleButton extends JToggleButton implements ItemListener {
		
		private final Language language;
		
		
		public LanguageToggleButton(Language language) {
			super(language.getIcon());
			
			this.language = language;
			
			setToolTipText(language.getName());
			setContentAreaFilled(false);
			setFocusPainted(false);
			
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			setPreferredSize(new Dimension(getIcon().getIconWidth(), getIcon().getIconHeight()));
			
			addItemListener(this);
		}
		

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2d = (Graphics2D) g;
			
			// make transparent if not selected
			if (!isSelected()) {
				AlphaComposite composite = AlphaComposite.SrcOver.derive(0.2f);
				g2d.setComposite(composite);
			}
			
			super.paintComponent(g2d);
		}
		

		@Override
		public void itemStateChanged(ItemEvent e) {
			LanguageSelectionPanel.this.setSelected(language, isSelected());
		}
		
	}
	

	private class LanguageFunction implements Function<SubtitlePackage, Language> {
		
		@Override
		public Language evaluate(SubtitlePackage sourceValue) {
			return sourceValue.getLanguage();
		}
		
	}
	
}
