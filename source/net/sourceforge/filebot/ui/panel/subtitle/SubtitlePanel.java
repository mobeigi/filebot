
package net.sourceforge.filebot.ui.panel.subtitle;


import static net.sourceforge.filebot.Settings.getApplicationName;
import static net.sourceforge.filebot.Settings.getApplicationVersion;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.Settings;
import net.sourceforge.filebot.ui.AbstractSearchPanel;
import net.sourceforge.filebot.ui.SelectDialog;
import net.sourceforge.filebot.web.OpenSubtitlesSubtitleClient;
import net.sourceforge.filebot.web.SearchResult;
import net.sourceforge.filebot.web.SubsceneSubtitleClient;
import net.sourceforge.filebot.web.SubtitleClient;
import net.sourceforge.filebot.web.SubtitleDescriptor;
import net.sourceforge.filebot.web.SubtitleSourceClient;
import net.sourceforge.tuned.ListChangeSynchronizer;
import net.sourceforge.tuned.ui.LabelProvider;
import net.sourceforge.tuned.ui.SimpleLabelProvider;


public class SubtitlePanel extends AbstractSearchPanel<SubtitleClient, SubtitlePackage> {
	
	public SubtitlePanel() {
		super("Subtitles", ResourceManager.getIcon("panel.subtitle"));
		
		historyPanel.setColumnHeader(0, "Show / Movie");
		historyPanel.setColumnHeader(1, "Number of Subtitles");
	}
	

	@Override
	protected List<SubtitleClient> createSearchEngines() {
		List<SubtitleClient> engines = new ArrayList<SubtitleClient>(2);
		
		engines.add(new OpenSubtitlesSubtitleClient(String.format("%s v%s", getApplicationName(), getApplicationVersion())));
		engines.add(new SubsceneSubtitleClient());
		engines.add(new SubtitleSourceClient());
		
		return engines;
	}
	

	@Override
	protected LabelProvider<SubtitleClient> createSearchEngineLabelProvider() {
		return SimpleLabelProvider.forClass(SubtitleClient.class);
	}
	

	@Override
	protected EventList<String> createSearchHistory() {
		// create in-memory history
		BasicEventList<String> history = new BasicEventList<String>();
		
		//  get the preferences node that contains the history entries
		//  and get a StringList that read and writes directly from and to the preferences
		List<String> persistentHistory = Settings.userRoot().node("subtitles/history").asList(String.class);
		
		// add history from the preferences to the current in-memory history (for completion)
		history.addAll(persistentHistory);
		
		// perform all insert/add/remove operations on the in-memory history on the preferences node as well 
		ListChangeSynchronizer.syncEventListToList(history, persistentHistory);
		
		return history;
	}
	

	@Override
	protected SubtitleRequestProcessor createRequestProcessor() {
		SubtitleClient client = searchTextField.getSelectButton().getSelectedValue();
		String text = searchTextField.getText().trim();
		
		//TODO language selection combobox
		Locale language = Locale.ENGLISH;
		
		return new SubtitleRequestProcessor(new SubtitleRequest(client, text, language));
	}
	
	
	protected static class SubtitleRequest extends Request {
		
		private final SubtitleClient client;
		private final Locale language;
		
		
		public SubtitleRequest(SubtitleClient client, String searchText, Locale language) {
			super(searchText);
			this.client = client;
			this.language = language;
		}
		

		public SubtitleClient getClient() {
			return client;
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
			return request.getClient().search(request.getSearchText());
		}
		

		@Override
		public Collection<SubtitlePackage> fetch() throws Exception {
			List<SubtitlePackage> packages = new ArrayList<SubtitlePackage>(20);
			
			for (SubtitleDescriptor subtitle : request.getClient().getSubtitleList(getSearchResult(), request.getLanguage())) {
				packages.add(new SubtitlePackage(subtitle));
			}
			
			return packages;
		}
		

		@Override
		public URI getLink() {
			return request.getClient().getSubtitleListLink(getSearchResult(), request.getLanguage());
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
		public String getTitle() {
			// add additional information to default title
			return String.format("%s [%s]", super.getTitle(), request.getLanguage().getDisplayName(Locale.ENGLISH));
		}
		

		@Override
		protected void configureSelectDialog(SelectDialog<SearchResult> selectDialog) {
			super.configureSelectDialog(selectDialog);
			selectDialog.getHeaderLabel().setText("Select a Show / Movie:");
		}
		
	}
	
}
