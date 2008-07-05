
package net.sourceforge.filebot.ui.panel.subtitle;


import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import net.sourceforge.filebot.ListChangeSynchronizer;
import net.sourceforge.filebot.Settings;
import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.filebot.ui.AbstractSearchPanel;
import net.sourceforge.filebot.ui.SelectDialog;
import net.sourceforge.filebot.web.SearchResult;
import net.sourceforge.filebot.web.SubtitleClient;
import net.sourceforge.filebot.web.SubtitleDescriptor;
import net.sourceforge.tuned.ui.SimpleIconProvider;


public class SubtitlePanel extends AbstractSearchPanel<SubtitleClient, SubtitlePackage, SubtitleDownloadPanel> {
	
	public SubtitlePanel() {
		super("Subtitle", ResourceManager.getIcon("panel.subtitle"));
		
		getHistoryPanel().setColumnHeader1("Show / Movie");
		getHistoryPanel().setColumnHeader2("Number of Subtitles");
		
		getSearchField().getSelectButton().setModel(SubtitleClient.getAvailableSubtitleClients());
		getSearchField().getSelectButton().setIconProvider(SimpleIconProvider.forClass(SubtitleClient.class));
		
		List<String> persistentSearchHistory = Settings.getSettings().asStringList(Settings.SUBTITLE_HISTORY);
		
		getSearchHistory().addAll(persistentSearchHistory);
		
		ListChangeSynchronizer.syncEventListToList(getSearchHistory(), persistentSearchHistory);
	}
	

	@Override
	protected SearchTask createSearchTask() {
		SubtitleDownloadPanel panel = new SubtitleDownloadPanel();
		
		return new SubtitleSearchTask(getSearchField().getSelected(), getSearchField().getText(), panel);
	}
	

	@Override
	protected void configureSelectDialog(SelectDialog<SearchResult> selectDialog) {
		selectDialog.setText("Select a Show / Movie:");
	}
	

	@Override
	protected FetchTask createFetchTask(SearchTask searchTask, SearchResult selectedSearchResult) {
		return new SubtitleFetchTask(searchTask.getClient(), selectedSearchResult, searchTask.getTabPanel());
	}
	

	@Override
	protected URI getLink(SubtitleClient client, SearchResult result) {
		return client.getSubtitleListLink(result);
	}
	
	
	private class SubtitleSearchTask extends SearchTask {
		
		public SubtitleSearchTask(SubtitleClient client, String searchText, SubtitleDownloadPanel panel) {
			super(client, searchText, panel);
		}
		

		@Override
		protected Collection<SearchResult> doInBackground() throws Exception {
			return getClient().search(getSearchText());
		}
		
	}
	

	private class SubtitleFetchTask extends FetchTask {
		
		public SubtitleFetchTask(SubtitleClient client, SearchResult searchResult, SubtitleDownloadPanel tabPanel) {
			super(client, searchResult, tabPanel);
		}
		

		@Override
		protected Collection<SubtitlePackage> fetch() throws Exception {
			//TODO language combobox
			Collection<SubtitleDescriptor> descriptors = getClient().getSubtitleList(getSearchResult(), Locale.ENGLISH);
			ArrayList<SubtitlePackage> packages = new ArrayList<SubtitlePackage>();
			
			for (SubtitleDescriptor descriptor : descriptors) {
				packages.add(new SubtitlePackage(descriptor));
			}
			
			return packages;
		}
		

		@Override
		protected void process(List<SubtitlePackage> elements) {
			getTabPanel().getPackagePanel().getModel().addAll(elements);
		}
		

		@Override
		public String getStatusMessage() {
			if (getCount() > 0)
				return String.format("%d subtitles", getCount());
			
			return "No subtitles found";
		}
	}
	
}
