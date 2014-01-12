package net.sourceforge.filebot.ui.episodelist;

import static net.sourceforge.filebot.ui.episodelist.SeasonSpinnerModel.*;
import static net.sourceforge.filebot.web.EpisodeUtilities.*;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;

import net.sourceforge.filebot.Analytics;
import net.sourceforge.filebot.Language;
import net.sourceforge.filebot.Settings;
import net.sourceforge.filebot.WebServices;
import net.sourceforge.filebot.media.MediaDetection;
import net.sourceforge.filebot.similarity.Normalization;
import net.sourceforge.filebot.ui.AbstractSearchPanel;
import net.sourceforge.filebot.ui.FileBotList;
import net.sourceforge.filebot.ui.FileBotListExportHandler;
import net.sourceforge.filebot.ui.FileBotTab;
import net.sourceforge.filebot.ui.LanguageComboBox;
import net.sourceforge.filebot.ui.SelectDialog;
import net.sourceforge.filebot.ui.transfer.ArrayTransferable;
import net.sourceforge.filebot.ui.transfer.ClipboardHandler;
import net.sourceforge.filebot.ui.transfer.CompositeTranserable;
import net.sourceforge.filebot.ui.transfer.FileExportHandler;
import net.sourceforge.filebot.web.Episode;
import net.sourceforge.filebot.web.EpisodeListProvider;
import net.sourceforge.filebot.web.SearchResult;
import net.sourceforge.filebot.web.SeasonOutOfBoundsException;
import net.sourceforge.filebot.web.SortOrder;
import net.sourceforge.tuned.StringUtilities;
import net.sourceforge.tuned.ui.LabelProvider;
import net.sourceforge.tuned.ui.SelectButton;
import net.sourceforge.tuned.ui.SimpleLabelProvider;
import net.sourceforge.tuned.ui.TunedUtilities;

public class EpisodeListPanel extends AbstractSearchPanel<EpisodeListProvider, Episode> {

	private SeasonSpinnerModel seasonSpinnerModel = new SeasonSpinnerModel();
	private LanguageComboBox languageComboBox = new LanguageComboBox(Language.getLanguage("en"), getSettings());
	private JComboBox sortOrderComboBox = new JComboBox(SortOrder.values());

	public EpisodeListPanel() {
		historyPanel.setColumnHeader(0, "Show");
		historyPanel.setColumnHeader(1, "Number of Episodes");

		JSpinner seasonSpinner = new JSpinner(seasonSpinnerModel);
		seasonSpinner.setEditor(new SeasonSpinnerEditor(seasonSpinner));

		// set minimum size to "All Seasons" preferred size
		seasonSpinner.setMinimumSize(seasonSpinner.getPreferredSize());

		// add after text field
		add(seasonSpinner, "sgy button, gap indent", 1);
		add(sortOrderComboBox, "sgy button, gap rel", 2);
		add(languageComboBox, "sgy button, gap indent+5", 3);

		searchTextField.getSelectButton().addPropertyChangeListener(SelectButton.SELECTED_VALUE, selectButtonListener);

		TunedUtilities.installAction(this, KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.SHIFT_MASK), new SpinSeasonAction(1));
		TunedUtilities.installAction(this, KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.SHIFT_MASK), new SpinSeasonAction(-1));
	}

	@Override
	protected Collection<String> getHistory(EpisodeListProvider engine) throws Exception {
		final List<String> names = new ArrayList<String>(100000);
		final SearchResult[] index = (engine == WebServices.AniDB) ? MediaDetection.releaseInfo.getAnidbIndex() : MediaDetection.releaseInfo.getTheTVDBIndex();
		for (SearchResult it : index) {
			for (String n : it.getEffectiveNames()) {
				names.add(Normalization.removeTrailingBrackets(n));
			}
		}
		return names;
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
		SortOrder order = (SortOrder) sortOrderComboBox.getSelectedItem();
		Locale language = languageComboBox.getModel().getSelectedItem().getLocale();

		return new EpisodeListRequestProcessor(new EpisodeListRequest(provider, text, season, order, language));
	};

	private final PropertyChangeListener selectButtonListener = new PropertyChangeListener() {

		@Override
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

		@Override
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

		public final EpisodeListProvider provider;
		public final int season;
		public final SortOrder order;
		public final Locale language;

		public EpisodeListRequest(EpisodeListProvider provider, String searchText, int season, SortOrder order, Locale language) {
			super(searchText);
			this.provider = provider;
			this.season = season;
			this.order = order;
			this.language = language;
		}
	}

	protected static class EpisodeListRequestProcessor extends RequestProcessor<EpisodeListRequest, Episode> {

		public EpisodeListRequestProcessor(EpisodeListRequest request) {
			super(request, new EpisodeListTab());
		}

		@Override
		public Collection<SearchResult> search() throws Exception {
			return request.provider.search(request.getSearchText(), request.language);
		}

		@Override
		public Collection<Episode> fetch() throws Exception {
			List<Episode> episodes = request.provider.getEpisodeList(getSearchResult(), request.order, request.language);

			if (request.season != ALL_SEASONS) {
				List<Episode> episodeForSeason = filterBySeason(episodes, request.season);
				if (episodeForSeason.isEmpty()) {
					throw new SeasonOutOfBoundsException(getSearchResult().getName(), request.season, getLastSeason(episodes));
				}
				episodes = episodeForSeason;
			}

			Analytics.trackEvent(request.provider.getName(), "ViewEpisodeList", getSearchResult().getName());
			return episodes;
		}

		@Override
		public URI getLink() {
			return request.provider.getEpisodeListLink(getSearchResult());
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
			if (request.season == ALL_SEASONS)
				return super.getTitle();

			// add additional information to default title
			return String.format("%s - Season %d", super.getTitle(), request.season);
		}

		@Override
		public Icon getIcon() {
			return request.provider.getIcon();
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
