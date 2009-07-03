
package net.sourceforge.filebot.ui.panel.subtitle;


import static net.sourceforge.filebot.Settings.*;
import static net.sourceforge.filebot.ui.panel.subtitle.LanguageComboBoxModel.*;

import java.awt.event.ItemEvent;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.prefs.Preferences;

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
import net.sourceforge.tuned.PreferencesMap.AbstractAdapter;
import net.sourceforge.tuned.PreferencesMap.PreferencesEntry;
import net.sourceforge.tuned.ui.LabelProvider;
import net.sourceforge.tuned.ui.SimpleLabelProvider;


public class SubtitlePanel extends AbstractSearchPanel<SubtitleProvider, SubtitlePackage> {
	
	private final LanguageComboBoxModel languageModel = new LanguageComboBoxModel();
	

	public SubtitlePanel() {
		historyPanel.setColumnHeader(0, "Show / Movie");
		historyPanel.setColumnHeader(1, "Number of Subtitles");
		
		JComboBox languageComboBox = new JComboBox(languageModel);
		
		languageComboBox.setRenderer(new LanguageComboBoxCellRenderer(languageComboBox.getRenderer()));
		
		// restore state
		languageModel.setSelectedItem(persistentSelectedLanguage.getValue());
		languageModel.favorites().addAll(0, persistentFavorites.getValue());
		
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
					persistentFavorites.setValue(languageModel.favorites());
				}
				
				persistentSelectedLanguage.setValue(language);
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
				new SublightSubtitleClient(getApplicationName(), Settings.userRoot().get("sublight.apikey")),
				new SubtitleSourceClient()
		};
	}
	

	@Override
	protected LabelProvider<SubtitleProvider> createSearchEngineLabelProvider() {
		return SimpleLabelProvider.forClass(SubtitleProvider.class);
	}
	

	@Override
	protected Settings getSettings() {
		return Settings.userRoot().node("subtitles");
	}
	

	@Override
	protected SubtitleRequestProcessor createRequestProcessor() {
		SubtitleProvider provider = searchTextField.getSelectButton().getSelectedValue();
		String text = searchTextField.getText().trim();
		Language language = languageModel.getSelectedItem();
		
		// null or proper language name
		String languageName = (language == ALL_LANGUAGES ? null : language.getName());
		
		return new SubtitleRequestProcessor(new SubtitleRequest(provider, text, languageName));
	}
	

	protected static class SubtitleRequest extends Request {
		
		private final SubtitleProvider provider;
		private final String languageName;
		

		public SubtitleRequest(SubtitleProvider provider, String searchText, String languageName) {
			super(searchText);
			
			this.provider = provider;
			this.languageName = languageName;
		}
		

		public SubtitleProvider getProvider() {
			return provider;
		}
		

		public String getLanguageName() {
			return languageName;
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
	

	private final PreferencesEntry<Language> persistentSelectedLanguage = getSettings().entry("language.selected", new AbstractAdapter<Language>() {
		
		@Override
		public Language get(Preferences prefs, String key) {
			return Language.getLanguage(prefs.get(key, ""));
		}
		

		@Override
		public void put(Preferences prefs, String key, Language value) {
			prefs.put(key, value == null ? "undefined" : value.getCode());
		}
	});
	
	private final PreferencesEntry<List<Language>> persistentFavorites = getSettings().entry("language.favorites", new AbstractAdapter<List<Language>>() {
		
		@Override
		public List<Language> get(Preferences prefs, String key) {
			List<Language> languages = new ArrayList<Language>();
			
			for (String languageCode : prefs.get(key, "").split("\\W+")) {
				languages.add(Language.getLanguage(languageCode));
			}
			
			return languages;
		}
		

		@Override
		public void put(Preferences prefs, String key, List<Language> languages) {
			StringBuilder sb = new StringBuilder();
			
			for (int i = 0; i < languages.size(); i++) {
				sb.append(languages.get(i).getCode());
				
				if (i < languages.size() - 1) {
					sb.append(",");
				}
			}
			
			prefs.put(key, sb.toString());
		}
	});
	
}
