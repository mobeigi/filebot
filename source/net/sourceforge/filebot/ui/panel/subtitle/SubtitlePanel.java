
package net.sourceforge.filebot.ui.panel.subtitle;


import static net.sourceforge.filebot.ui.Language.*;
import static net.sourceforge.filebot.ui.panel.subtitle.LanguageComboBoxModel.*;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ItemEvent;
import java.awt.geom.Path2D;
import java.net.URI;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import javax.swing.Icon;
import javax.swing.JComboBox;

import net.sourceforge.filebot.Settings;
import net.sourceforge.filebot.WebServices;
import net.sourceforge.filebot.ui.AbstractSearchPanel;
import net.sourceforge.filebot.ui.Language;
import net.sourceforge.filebot.ui.SelectDialog;
import net.sourceforge.filebot.web.SearchResult;
import net.sourceforge.filebot.web.SubtitleDescriptor;
import net.sourceforge.filebot.web.SubtitleProvider;
import net.sourceforge.filebot.web.VideoHashSubtitleService;
import net.sourceforge.tuned.PreferencesList;
import net.sourceforge.tuned.PreferencesMap.PreferencesEntry;
import net.sourceforge.tuned.ui.LabelProvider;
import net.sourceforge.tuned.ui.SimpleLabelProvider;


public class SubtitlePanel extends AbstractSearchPanel<SubtitleProvider, SubtitlePackage> {
	
	private final LanguageComboBoxModel languageModel = new LanguageComboBoxModel();
	
	private static final PreferencesEntry<String> persistentSelectedLanguage = Settings.forPackage(SubtitlePanel.class).entry("language.selected");
	private static final PreferencesList<String> persistentFavoriteLanguages = Settings.forPackage(SubtitlePanel.class).node("language.favorites").asList();
	

	public SubtitlePanel() {
		historyPanel.setColumnHeader(0, "Show / Movie");
		historyPanel.setColumnHeader(1, "Number of Subtitles");
		
		JComboBox languageComboBox = new JComboBox(languageModel);
		
		languageComboBox.setRenderer(new LanguageComboBoxCellRenderer(languageComboBox.getRenderer()));
		
		// restore selected language
		languageModel.setSelectedItem(Language.getLanguage(persistentSelectedLanguage.getValue()));
		
		// restore favorite languages
		for (String favoriteLanguage : persistentFavoriteLanguages) {
			languageModel.favorites().add(languageModel.favorites().size(), getLanguage(favoriteLanguage));
		}
		
		// guess favorite languages
		if (languageModel.favorites().isEmpty()) {
			for (Locale locale : new Locale[] { Locale.getDefault(), Locale.ENGLISH }) {
				languageModel.favorites().add(getLanguage(locale.getLanguage()));
			}
		}
		
		// update favorites on change
		languageComboBox.addPopupMenuListener(new PopupSelectionListener() {
			
			@Override
			public void itemStateChanged(ItemEvent e) {
				Language language = (Language) e.getItem();
				
				if (languageModel.favorites().add(language)) {
					persistentFavoriteLanguages.set(new AbstractList<String>() {
						
						@Override
						public String get(int index) {
							return languageModel.favorites().get(index).getCode();
						}
						

						@Override
						public int size() {
							return languageModel.favorites().size();
						}
					});
				}
				
				persistentSelectedLanguage.setValue(language.getCode());
			}
		});
		
		// add after text field
		add(languageComboBox, 1);
		
		// add at the top right corner
		add(dropTarget, "width 1.6cm!, height 1.2cm!, pos n 0% 100% n", 0);
	}
	

	private final SubtitleDropTarget dropTarget = new SubtitleDropTarget() {
		
		@Override
		public VideoHashSubtitleService[] getServices() {
			return WebServices.getVideoHashSubtitleServices();
		}
		

		@Override
		public String getQueryLanguage() {
			// use currently selected language for drop target
			return languageModel.getSelectedItem() == ALL_LANGUAGES ? null : languageModel.getSelectedItem().getName();
		}
		

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2d = (Graphics2D) g.create();
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			
			Path2D path = new Path2D.Float();
			path.moveTo(0, 0);
			path.lineTo(0, getHeight() - 1 - 12);
			path.quadTo(0, getHeight() - 1, 12, getHeight() - 1);
			path.lineTo(getWidth(), getHeight() - 1);
			path.lineTo(getWidth(), 0);
			
			g2d.setPaint(getBackground());
			g2d.fill(path);
			
			g2d.setPaint(Color.gray);
			g2d.draw(path);
			
			g2d.translate(2, 0);
			super.paintComponent(g2d);
			g2d.dispose();
		}
	};
	

	@Override
	protected SubtitleProvider[] getSearchEngines() {
		return WebServices.getSubtitleProviders();
	}
	

	@Override
	protected LabelProvider<SubtitleProvider> getSearchEngineLabelProvider() {
		return SimpleLabelProvider.forClass(SubtitleProvider.class);
	}
	

	@Override
	protected Settings getSettings() {
		return Settings.forPackage(SubtitlePanel.class);
	}
	

	@Override
	protected SubtitleRequestProcessor createRequestProcessor() {
		SubtitleProvider provider = searchTextField.getSelectButton().getSelectedValue();
		String text = searchTextField.getText().trim();
		Language language = languageModel.getSelectedItem();
		
		return new SubtitleRequestProcessor(new SubtitleRequest(provider, text, language));
	}
	

	protected static class SubtitleRequest extends Request {
		
		private final SubtitleProvider provider;
		private final Language language;
		

		public SubtitleRequest(SubtitleProvider provider, String searchText, Language language) {
			super(searchText);
			
			this.provider = provider;
			this.language = language;
		}
		

		public SubtitleProvider getProvider() {
			return provider;
		}
		

		public String getLanguageName() {
			return language == ALL_LANGUAGES ? null : language.getName();
		}
		
	}
	

	protected static class SubtitleRequestProcessor extends RequestProcessor<SubtitleRequest, SubtitlePackage> {
		
		public SubtitleRequestProcessor(SubtitleRequest request) {
			super(request, new SubtitleDownloadComponent());
		}
		

		@Override
		public Collection<SearchResult> search() throws Exception {
			return request.getProvider().search(request.getSearchText());
		}
		

		@Override
		public Collection<SubtitlePackage> fetch() throws Exception {
			List<SubtitlePackage> packages = new ArrayList<SubtitlePackage>();
			
			for (SubtitleDescriptor subtitle : request.getProvider().getSubtitleList(getSearchResult(), request.getLanguageName())) {
				packages.add(new SubtitlePackage(subtitle));
			}
			
			return packages;
		}
		

		@Override
		public URI getLink() {
			return request.getProvider().getSubtitleListLink(getSearchResult(), request.getLanguageName());
		}
		

		@Override
		public void process(Collection<SubtitlePackage> subtitles) {
			getComponent().setLanguageVisible(request.getLanguageName() == null);
			getComponent().getPackageModel().addAll(subtitles);
		}
		

		@Override
		public SubtitleDownloadComponent getComponent() {
			return (SubtitleDownloadComponent) super.getComponent();
		}
		

		@Override
		public String getStatusMessage(Collection<SubtitlePackage> result) {
			return (result.isEmpty()) ? "No subtitles found" : String.format("%d subtitles", result.size());
		}
		

		@Override
		public Icon getIcon() {
			return request.provider.getIcon();
		}
		

		@Override
		protected void configureSelectDialog(SelectDialog<SearchResult> selectDialog) {
			super.configureSelectDialog(selectDialog);
			selectDialog.getHeaderLabel().setText("Select a Show / Movie:");
		}
		
	}
	
}
