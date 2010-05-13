
package net.sourceforge.filebot.ui.panel.episodelist;


import static net.sourceforge.filebot.ui.panel.episodelist.SeasonSpinnerModel.*;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;

import net.sourceforge.filebot.Settings;
import net.sourceforge.filebot.WebServices;
import net.sourceforge.filebot.ui.AbstractSearchPanel;
import net.sourceforge.filebot.ui.FileBotList;
import net.sourceforge.filebot.ui.FileBotListExportHandler;
import net.sourceforge.filebot.ui.FileBotTab;
import net.sourceforge.filebot.ui.SelectDialog;
import net.sourceforge.filebot.ui.transfer.ArrayTransferable;
import net.sourceforge.filebot.ui.transfer.ClipboardHandler;
import net.sourceforge.filebot.ui.transfer.CompositeTranserable;
import net.sourceforge.filebot.ui.transfer.FileExportHandler;
import net.sourceforge.filebot.ui.transfer.SaveAction;
import net.sourceforge.filebot.web.Episode;
import net.sourceforge.filebot.web.EpisodeListProvider;
import net.sourceforge.filebot.web.SearchResult;
import net.sourceforge.tuned.StringUtilities;
import net.sourceforge.tuned.ui.LabelProvider;
import net.sourceforge.tuned.ui.SelectButton;
import net.sourceforge.tuned.ui.SimpleLabelProvider;
import net.sourceforge.tuned.ui.TunedUtilities;


public class EpisodeListPanel extends AbstractSearchPanel<EpisodeListProvider, Episode> {
	
	private SeasonSpinnerModel seasonSpinnerModel = new SeasonSpinnerModel();
	

	public EpisodeListPanel() {
		historyPanel.setColumnHeader(0, "Show");
		historyPanel.setColumnHeader(1, "Number of Episodes");
		
		JSpinner seasonSpinner = new JSpinner(seasonSpinnerModel);
		seasonSpinner.setEditor(new SeasonSpinnerEditor(seasonSpinner));
		
		// set minimum size to "All Seasons" preferred size
		seasonSpinner.setMinimumSize(seasonSpinner.getPreferredSize());
		
		// add after text field
		add(seasonSpinner, 1);
		
		// add after tabbed pane
		tabbedPaneGroup.add(new JButton(new SaveAction(new SelectedTabExportHandler())));
		
		searchTextField.getSelectButton().addPropertyChangeListener(SelectButton.SELECTED_VALUE, selectButtonListener);
		
		TunedUtilities.installAction(this, KeyStroke.getKeyStroke("shift UP"), new SpinSeasonAction(1));
		TunedUtilities.installAction(this, KeyStroke.getKeyStroke("shift DOWN"), new SpinSeasonAction(-1));
	}
	

	@Override
	protected EpisodeListProvider[] getSearchEngines() {
		return WebServices.getEpisodeListProviders();
	}
	

	@Override
	protected LabelProvider<EpisodeListProvider> getSearchEngineLabelProvider() {
		return SimpleLabelProvider.forClass(EpisodeListProvider.class);
	}
	

	@Override
	protected Settings getSettings() {
		return Settings.forPackage(EpisodeListPanel.class);
	}
	

	@Override
	protected EpisodeListRequestProcessor createRequestProcessor() {
		EpisodeListProvider provider = searchTextField.getSelectButton().getSelectedValue();
		String text = searchTextField.getText().trim();
		int season = seasonSpinnerModel.getSeason();
		
		return new EpisodeListRequestProcessor(new EpisodeListRequest(provider, text, season));
	};
	

	private final PropertyChangeListener selectButtonListener = new PropertyChangeListener() {
		
		public void propertyChange(PropertyChangeEvent evt) {
			EpisodeListProvider provider = searchTextField.getSelectButton().getSelectedValue();
			
			// lock season spinner on "All Seasons" if provider doesn't support fetching of single seasons
			if (!provider.hasSingleSeasonSupport()) {
				seasonSpinnerModel.lock(ALL_SEASONS);
			} else {
				seasonSpinnerModel.unlock();
			}
		}
	};
	

	private class SpinSeasonAction extends AbstractAction {
		
		public SpinSeasonAction(int spin) {
			super(String.format("Spin%+d", spin));
			putValue("spin", spin);
		}
		

		public void actionPerformed(ActionEvent e) {
			seasonSpinnerModel.spin((Integer) getValue("spin"));
		}
	}
	

	private class SelectedTabExportHandler implements FileExportHandler {
		
		/**
		 * @return the <code>FileExportHandler</code> of the currently selected tab
		 */
		@SuppressWarnings("unchecked")
		private FileExportHandler getExportHandler() {
			try {
				EpisodeListTab list = ((FileBotTab<EpisodeListTab>) tabbedPane.getSelectedComponent()).getComponent();
				return list.getExportHandler();
			} catch (ClassCastException e) {
				// selected component is the history panel
				return null;
			}
		}
		

		@Override
		public boolean canExport() {
			FileExportHandler handler = getExportHandler();
			
			if (handler == null)
				return false;
			
			return handler.canExport();
		}
		

		@Override
		public void export(File file) throws IOException {
			getExportHandler().export(file);
		}
		

		@Override
		public String getDefaultFileName() {
			return getExportHandler().getDefaultFileName();
		}
		
	}
	

	protected static class EpisodeListRequest extends Request {
		
		private final EpisodeListProvider provider;
		private final int season;
		

		public EpisodeListRequest(EpisodeListProvider provider, String searchText, int season) {
			super(searchText);
			this.provider = provider;
			this.season = season;
		}
		

		public EpisodeListProvider getProvider() {
			return provider;
		}
		

		public int getSeason() {
			return season;
		}
		
	}
	

	protected static class EpisodeListRequestProcessor extends RequestProcessor<EpisodeListRequest, Episode> {
		
		public EpisodeListRequestProcessor(EpisodeListRequest request) {
			super(request, new EpisodeListTab());
		}
		

		@Override
		public Collection<SearchResult> search() throws Exception {
			return request.getProvider().search(request.getSearchText());
		}
		

		@Override
		public Collection<Episode> fetch() throws Exception {
			if (request.getSeason() != ALL_SEASONS)
				return request.getProvider().getEpisodeList(getSearchResult(), request.getSeason());
			else
				return request.getProvider().getEpisodeList(getSearchResult());
		}
		

		@Override
		public URI getLink() {
			if (request.getSeason() != ALL_SEASONS) {
				return request.getProvider().getEpisodeListLink(getSearchResult(), request.getSeason());
			}
			
			return request.getProvider().getEpisodeListLink(getSearchResult());
		}
		

		@Override
		public void process(Collection<Episode> episodes) {
			// set a proper title for the export handler before adding episodes
			getComponent().setTitle(getTitle());
			
			getComponent().getModel().addAll(episodes);
		}
		

		@Override
		public String getStatusMessage(Collection<Episode> result) {
			return (result.isEmpty()) ? "No episodes found" : String.format("%d episodes", result.size());
		}
		

		@Override
		public EpisodeListTab getComponent() {
			return (EpisodeListTab) super.getComponent();
		}
		

		@Override
		public String getTitle() {
			if (request.getSeason() == ALL_SEASONS)
				return super.getTitle();
			
			// add additional information to default title
			return String.format("%s - Season %d", super.getTitle(), request.getSeason());
		}
		

		@Override
		public Icon getIcon() {
			return request.getProvider().getIcon();
		}
		

		@Override
		protected void configureSelectDialog(SelectDialog<SearchResult> selectDialog) {
			super.configureSelectDialog(selectDialog);
			selectDialog.getHeaderLabel().setText("Select a Show:");
		}
		
	}
	

	protected static class EpisodeListTab extends FileBotList<Episode> {
		
		public EpisodeListTab() {
			// initialize dnd and clipboard export handler for episode list
			setExportHandler(new EpisodeListExportHandler(this));
			getTransferHandler().setClipboardHandler(new EpisodeListExportHandler(this));
			
			// allow removal of episode list entries
			getRemoveAction().setEnabled(true);
			
			// remove borders
			listScrollPane.setBorder(null);
			setBorder(null);
		}
		
	}
	

	protected static class EpisodeListExportHandler extends FileBotListExportHandler implements ClipboardHandler {
		
		public EpisodeListExportHandler(FileBotList<Episode> list) {
			super(list);
		}
		

		@Override
		public Transferable createTransferable(JComponent c) {
			Transferable episodeArray = new ArrayTransferable<Episode>(list.getModel().toArray(new Episode[0]));
			Transferable textFile = super.createTransferable(c);
			
			return new CompositeTranserable(episodeArray, textFile);
		}
		

		@Override
		public void exportToClipboard(JComponent c, Clipboard clipboard, int action) throws IllegalStateException {
			Object[] selection = list.getListComponent().getSelectedValues();
			Episode[] episodes = Arrays.copyOf(selection, selection.length, Episode[].class);
			
			Transferable episodeArray = new ArrayTransferable<Episode>(episodes);
			Transferable stringSelection = new StringSelection(StringUtilities.join(episodes, "\n"));
			
			clipboard.setContents(new CompositeTranserable(episodeArray, stringSelection), null);
		}
	}
	
}
