
package net.sourceforge.filebot.ui.panel.subtitle;


import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolTip;
import javax.swing.ListModel;

import net.sourceforge.filebot.Settings;
import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.filebot.web.SubtitleDescriptor;
import net.sourceforge.tuned.ui.IconViewPanel;
import net.sourceforge.tuned.ui.SimpleListModel;


public class SubtitleViewPanel extends IconViewPanel {
	
	private ListModel unfilteredModel = new SimpleListModel();
	private JPanel languageFilterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 1));
	
	private Map<String, Boolean> languageFilterSelection = new TreeMap<String, Boolean>(String.CASE_INSENSITIVE_ORDER);
	
	
	public SubtitleViewPanel() {
		setCellRenderer(new SubtitleCellRenderer());
		
		languageFilterPanel.setOpaque(false);
		
		getHeaderPanel().add(languageFilterPanel, BorderLayout.EAST);
		
		languageFilterSelection.putAll(Settings.getSettings().getBooleanMap(Settings.SUBTITLE_LANGUAGE));
		
	}
	

	@Override
	public void setModel(ListModel model) {
		unfilteredModel = model;
		
		updateLanguageFilterButtonPanel();
		
		updateFilteredModel();
	}
	

	@Override
	public ListModel getModel() {
		return unfilteredModel;
	}
	

	private void updateLanguageFilterButtonPanel() {
		
		SortedSet<String> languages = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
		
		for (int i = 0; i < unfilteredModel.getSize(); i++) {
			SubtitleDescriptor subtitle = (SubtitleDescriptor) unfilteredModel.getElementAt(i);
			languages.add(subtitle.getLanguageName());
		}
		
		languageFilterPanel.removeAll();
		
		for (String language : languages) {
			LanguageFilterButton languageFilterButton = createLanguageFilterButton(language);
			languageFilterButton.addItemListener(new LanguageFilterItemListener(language));
			
			languageFilterPanel.add(languageFilterButton);
		}
	}
	

	private void updateFilteredModel() {
		SimpleListModel model = new SimpleListModel();
		
		for (int i = 0; i < unfilteredModel.getSize(); i++) {
			SubtitleDescriptor subtitle = (SubtitleDescriptor) unfilteredModel.getElementAt(i);
			
			if (isLanguageSelected(subtitle.getLanguageName())) {
				model.add(subtitle);
			}
		}
		
		super.setModel(model);
	}
	

	public boolean isLanguageSelected(String language) {
		return !languageFilterSelection.containsKey(language) || languageFilterSelection.get(language);
	}
	

	private LanguageFilterButton createLanguageFilterButton(String language) {
		Locale locale = LanguageResolver.getDefault().getLocale(language);
		
		boolean selected = isLanguageSelected(language);
		
		if (locale != null)
			return new LanguageFilterButton(locale, selected);
		else
			return new LanguageFilterButton(language, selected);
	}
	
	
	private class LanguageFilterItemListener implements ItemListener {
		
		private final String language;
		
		
		public LanguageFilterItemListener(String language) {
			this.language = language;
		}
		

		@Override
		public void itemStateChanged(ItemEvent e) {
			boolean selected = (e.getStateChange() == ItemEvent.SELECTED);
			
			languageFilterSelection.put(language, selected);
			Settings.getSettings().putBooleanMapEntry(Settings.SUBTITLE_LANGUAGE, language, selected);
			
			updateFilteredModel();
		}
		
	};
	

	private class LanguageFilterButton extends JToggleButton {
		
		public LanguageFilterButton(Locale locale, boolean selected) {
			this(locale.getDisplayLanguage(Locale.ENGLISH), ResourceManager.getFlagIcon(locale.getLanguage()), selected);
		}
		

		public LanguageFilterButton(String language, boolean selected) {
			this(language, ResourceManager.getFlagIcon(null), selected);
		}
		

		public LanguageFilterButton(String language, Icon icon, boolean selected) {
			super(icon, selected);
			
			setToolTipText(language);
			setContentAreaFilled(false);
			setFocusPainted(false);
			
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			
			setPreferredSize(new Dimension(icon.getIconWidth(), icon.getIconHeight()));
		}
		

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2d = (Graphics2D) g;
			
			AlphaComposite composite = AlphaComposite.SrcOver.derive(isSelected() ? 1.0f : 0.2f);
			g2d.setComposite(composite);
			
			super.paintComponent(g2d);
		}
		
	}
	
	
	@Override
	public JToolTip createToolTip() {
		System.out.println("SubtitleViewPanel.createToolTip()");
		return super.createToolTip();
	}
	
}
