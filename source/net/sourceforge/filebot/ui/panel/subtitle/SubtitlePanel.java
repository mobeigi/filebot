
package net.sourceforge.filebot.ui.panel.subtitle;


import static net.sourceforge.filebot.Settings.getApplicationName;
import static net.sourceforge.filebot.Settings.getApplicationVersion;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import javax.swing.Icon;

import net.sourceforge.filebot.Settings;
import net.sourceforge.filebot.ui.AbstractSearchPanel;
import net.sourceforge.filebot.ui.SelectDialog;
import net.sourceforge.filebot.web.OpenSubtitlesSubtitleClient;
import net.sourceforge.filebot.web.SearchResult;
import net.sourceforge.filebot.web.SubsceneSubtitleClient;
import net.sourceforge.filebot.web.SubtitleDescriptor;
import net.sourceforge.filebot.web.SubtitleProvider;
import net.sourceforge.filebot.web.SubtitleSourceClient;
import net.sourceforge.tuned.ui.LabelProvider;
import net.sourceforge.tuned.ui.SimpleLabelProvider;


public class SubtitlePanel extends AbstractSearchPanel<SubtitleProvider, SubtitlePackage> {
	
	public SubtitlePanel() {
		historyPanel.setColumnHeader(0, "Show / Movie");
		historyPanel.setColumnHeader(1, "Number of Subtitles");
	}
	

	@Override
	protected List<SubtitleProvider> createSearchEngines() {
		List<SubtitleProvider> engines = new ArrayList<SubtitleProvider>(2);
		
		engines.add(new OpenSubtitlesSubtitleClient(String.format("%s v%s", getApplicationName(), getApplicationVersion())));
		engines.add(new SubsceneSubtitleClient());
		engines.add(new SubtitleSourceClient());
		
		return engines;
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
		
		//TODO language selection combobox
		Locale language = Locale.ENGLISH;
		
		return new SubtitleRequestProcessor(new SubtitleRequest(provider, text, language));
	}
	
	
	protected static class SubtitleRequest extends Request {
		
		private final SubtitleProvider provider;
		private final Locale language;
		
		
		public SubtitleRequest(SubtitleProvider provider, String searchText, Locale language) {
			super(searchText);
			this.provider = provider;
			this.language = language;
		}
		

		public SubtitleProvider getProvider() {
			return provider;
		}
		

		public Locale getLanguage() {
			return language;
		}
		
	}
	

	protected static class SubtitleRequestProcessor extends RequestProcessor<SubtitleRequest, SubtitlePackage> {
		
		public SubtitleRequestProcessor(SubtitleRequest request) {
			super(request, new SubtitleDownloadPanel());
		}
		

		@Override
		public Collection<SearchResult> search() throws Exception {
			return request.getProvider().search(request.getSearchText());
		}
		

		@Override
		public Collection<SubtitlePackage> fetch() throws Exception {
			List<SubtitlePackage> packages = new ArrayList<SubtitlePackage>(20);
			
			for (SubtitleDescriptor subtitle : request.getProvider().getSubtitleList(getSearchResult(), request.getLanguage())) {
				packages.add(new SubtitlePackage(subtitle));
			}
			
			return packages;
		}
		

		@Override
		public URI getLink() {
			return request.getProvider().getSubtitleListLink(getSearchResult(), request.getLanguage());
		}
		

		@Override
		public void process(Collection<SubtitlePackage> subtitles) {
			getComponent().getPackagePanel().getModel().addAll(subtitles);
		}
		

		@Override
		public SubtitleDownloadPanel getComponent() {
			return (SubtitleDownloadPanel) super.getComponent();
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
