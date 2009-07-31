
package net.sourceforge.filebot.ui.panel.subtitle;


import static net.sourceforge.filebot.Settings.*;
import static net.sourceforge.filebot.ui.panel.subtitle.LanguageComboBoxModel.*;

import java.awt.event.ItemEvent;
import java.net.URI;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import javax.swing.Icon;
import javax.swing.JComboBox;

import net.sourceforge.filebot.Settings;
import net.sourceforge.filebot.ui.AbstractSearchPanel;
import net.sourceforge.filebot.ui.SelectDialog;
import net.sourceforge.filebot.web.OpenSubtitlesClient;
import net.sourceforge.filebot.web.SearchResult;
import net.sourceforge.filebot.web.SublightSubtitleClient;
import net.sourceforge.filebot.web.SubsceneSubtitleClient;
import net.sourceforge.filebot.web.SubtitleDescriptor;
import net.sourceforge.filebot.web.SubtitleProvider;
import net.sourceforge.filebot.web.SubtitleSourceClient;
import net.sourceforge.tuned.PreferencesList;
import net.sourceforge.tuned.PreferencesMap.PreferencesEntry;
import net.sourceforge.tuned.ui.LabelProvider;
import net.sourceforge.tuned.ui.SimpleLabelProvider;


public class SubtitlePanel extends AbstractSearchPanel<SubtitleProvider, SubtitlePackage> {
	
	private final LanguageComboBoxModel languageModel = new LanguageComboBoxModel();
	
	private final PreferencesEntry<String> persistentSelectedLanguage = Settings.forPackage(this).entry("language.selected");
	private final PreferencesList<String> persistentFavoriteLanguages = Settings.forPackage(this).node("language.favorites").asList();
	

	public SubtitlePanel() {
		historyPanel.setColumnHeader(0, "Show / Movie");
		historyPanel.setColumnHeader(1, "Number of Subtitles");
		
		JComboBox languageComboBox = new JComboBox(languageModel);
		
		languageComboBox.setRenderer(new LanguageComboBoxCellRenderer(languageComboBox.getRenderer()));
		
		// restore selected language
		languageModel.setSelectedItem(Language.getLanguage(persistentSelectedLanguage.getValue()));
		
		// restore favorite languages
		for (String favoriteLanguage : persistentFavoriteLanguages) {
			languageModel.favorites().add(languageModel.favorites().size(), Language.getLanguage(favoriteLanguage));
		}
		
		// guess favorite languages
		if (languageModel.favorites().isEmpty()) {
			for (Locale locale : new Locale[] { Locale.getDefault(), Locale.ENGLISH }) {
				languageModel.favorites().add(Language.getLanguage(locale.getLanguage()));
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
	}
	

	@Override
	protected SubtitleProvider[] createSearchEngines() {
		return new SubtitleProvider[] {
				new OpenSubtitlesClient(String.format("%s %s", getApplicationName(), getApplicationVersion())),
				new SubsceneSubtitleClient(),
				new SublightSubtitleClient(getApplicationName(), getApplicationProperty("sublight.apikey")),
				new SubtitleSourceClient()
		};
	}
	

	@Override
	protected LabelProvider<SubtitleProvider> createSearchEngineLabelProvider() {
		return SimpleLabelProvider.forClass(SubtitleProvider.class);
	}
	

	@Override
	protected Settings getSettings() {
		return Settings.forPackage(this);
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
