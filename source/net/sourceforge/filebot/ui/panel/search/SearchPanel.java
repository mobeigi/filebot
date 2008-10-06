
package net.sourceforge.filebot.ui.panel.search;


import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.filebot.ui.FileBotList;
import net.sourceforge.filebot.ui.FileBotPanel;
import net.sourceforge.filebot.ui.HistoryPanel;
import net.sourceforge.filebot.ui.MessageManager;
import net.sourceforge.filebot.ui.SelectDialog;
import net.sourceforge.filebot.ui.transfer.FileExportHandler;
import net.sourceforge.filebot.ui.transfer.SaveAction;
import net.sourceforge.filebot.web.AnidbClient;
import net.sourceforge.filebot.web.Episode;
import net.sourceforge.filebot.web.EpisodeListClient;
import net.sourceforge.filebot.web.SearchResult;
import net.sourceforge.filebot.web.TVDotComClient;
import net.sourceforge.filebot.web.TVRageClient;
import net.sourceforge.tuned.ExceptionUtil;
import net.sourceforge.tuned.ui.LabelProvider;
import net.sourceforge.tuned.ui.SelectButton;
import net.sourceforge.tuned.ui.SelectButtonTextField;
import net.sourceforge.tuned.ui.SimpleLabelProvider;
import net.sourceforge.tuned.ui.SwingWorkerPropertyChangeAdapter;
import net.sourceforge.tuned.ui.TunedUtil;


public class SearchPanel extends FileBotPanel {
	
	private JTabbedPane tabbedPane = new JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
	
	private HistoryPanel historyPanel = new HistoryPanel();
	
	private SeasonSpinnerModel seasonSpinnerModel = new SeasonSpinnerModel();
	
	private SelectButtonTextField<EpisodeListClient> searchField;
	
	
	public SearchPanel() {
		super("Search", ResourceManager.getIcon("panel.search"));
		
		searchField = new SelectButtonTextField<EpisodeListClient>();
		
		searchField.getSelectButton().setModel(createSearchEngines());
		searchField.getSelectButton().setLabelProvider(createSearchEngineLabelProvider());
		
		searchField.getSelectButton().addPropertyChangeListener(SelectButton.SELECTED_VALUE, selectButtonListener);
		
		historyPanel.setColumnHeader1("Show");
		historyPanel.setColumnHeader2("Number of Episodes");
		historyPanel.setColumnHeader3("Duration");
		
		JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
		
		Box searchBox = Box.createHorizontalBox();
		searchBox.setBorder(new EmptyBorder(5, 5, 5, 5));
		
		JSpinner seasonSpinner = new JSpinner(seasonSpinnerModel);
		seasonSpinner.setEditor(new SeasonSpinnerEditor(seasonSpinner));
		searchField.setMaximumSize(searchField.getPreferredSize());
		seasonSpinner.setMaximumSize(seasonSpinner.getPreferredSize());
		
		searchBox.add(Box.createHorizontalGlue());
		searchBox.add(searchField);
		searchBox.add(Box.createHorizontalStrut(15));
		searchBox.add(seasonSpinner);
		searchBox.add(Box.createHorizontalStrut(15));
		searchBox.add(new JButton(searchAction));
		searchBox.add(Box.createHorizontalGlue());
		
		JPanel centerPanel = new JPanel(new BorderLayout());
		centerPanel.setBorder(BorderFactory.createTitledBorder("Search Results"));
		
		Box buttonBox = Box.createHorizontalBox();
		buttonBox.setBorder(new EmptyBorder(5, 5, 5, 5));
		buttonBox.add(Box.createHorizontalGlue());
		buttonBox.add(new JButton(saveAction));
		buttonBox.add(Box.createHorizontalGlue());
		
		centerPanel.add(tabbedPane, BorderLayout.CENTER);
		centerPanel.add(buttonBox, BorderLayout.SOUTH);
		
		JScrollPane historyScrollPane = new JScrollPane(historyPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		historyScrollPane.setBorder(BorderFactory.createEmptyBorder());
		
		tabbedPane.addTab("History", ResourceManager.getIcon("tab.history"), historyScrollPane);
		
		mainPanel.add(searchBox, BorderLayout.NORTH);
		mainPanel.add(centerPanel, BorderLayout.CENTER);
		
		this.add(mainPanel, BorderLayout.CENTER);
		
		TunedUtil.putActionForKeystroke(this, KeyStroke.getKeyStroke("ENTER"), searchAction);
		TunedUtil.putActionForKeystroke(this, KeyStroke.getKeyStroke("UP"), upAction);
		TunedUtil.putActionForKeystroke(this, KeyStroke.getKeyStroke("DOWN"), downAction);
	}
	

	protected List<EpisodeListClient> createSearchEngines() {
		List<EpisodeListClient> engines = new ArrayList<EpisodeListClient>(3);
		
		engines.add(new TVDotComClient());
		engines.add(new AnidbClient());
		engines.add(new TVRageClient());
		
		return engines;
	}
	

	protected LabelProvider<EpisodeListClient> createSearchEngineLabelProvider() {
		return SimpleLabelProvider.forClass(EpisodeListClient.class);
	}
	
	private final PropertyChangeListener selectButtonListener = new PropertyChangeListener() {
		
		public void propertyChange(PropertyChangeEvent evt) {
			EpisodeListClient client = searchField.getSelected();
			
			if (!client.hasSingleSeasonSupport()) {
				seasonSpinnerModel.lock(SeasonSpinnerModel.ALL_SEASONS);
			} else {
				seasonSpinnerModel.unlock();
			}
		}
		
	};
	
	private final AbstractAction searchAction = new AbstractAction("Find", ResourceManager.getIcon("action.find")) {
		
		public void actionPerformed(ActionEvent e) {
			EpisodeListClient searchEngine = searchField.getSelected();
			
			SearchTask task = new SearchTask(searchEngine, searchField.getText(), seasonSpinnerModel.getSeason());
			task.addPropertyChangeListener(new SearchTaskListener());
			
			task.execute();
		}
	};
	
	private final AbstractAction upAction = new AbstractAction("Up") {
		
		public void actionPerformed(ActionEvent e) {
			seasonSpinnerModel.setValue(seasonSpinnerModel.getNextValue());
		}
	};
	
	private final AbstractAction downAction = new AbstractAction("Down") {
		
		public void actionPerformed(ActionEvent e) {
			seasonSpinnerModel.setValue(seasonSpinnerModel.getPreviousValue());
		}
	};
	
	private final SaveAction saveAction = new SaveAction(new SelectedTabExportHandler());
	
	
	private class SelectedTabExportHandler implements FileExportHandler {
		
		/**
		 * @return the <code>FileExportHandler</code> of the currently selected tab
		 */
		private FileExportHandler getExportHandler() {
			try {
				FileBotList<?> list = (FileBotList<?>) tabbedPane.getSelectedComponent();
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
	

	private class SearchTask extends SwingWorker<Collection<SearchResult>, Void> {
		
		private final String query;
		private final EpisodeListClient client;
		private final int numberOfSeason;
		
		
		public SearchTask(EpisodeListClient client, String query, int numberOfSeason) {
			this.query = query;
			this.client = client;
			this.numberOfSeason = numberOfSeason;
		}
		

		@Override
		protected Collection<SearchResult> doInBackground() throws Exception {
			return client.search(query);
		}
		
	}
	

	private class SearchTaskListener extends SwingWorkerPropertyChangeAdapter {
		
		private EpisodeListPanel episodeList;
		
		
		@Override
		public void started(PropertyChangeEvent evt) {
			SearchTask task = (SearchTask) evt.getSource();
			
			episodeList = new EpisodeListPanel();
			
			String title = task.query;
			
			if (task.numberOfSeason != SeasonSpinnerModel.ALL_SEASONS) {
				title += String.format(" - Season %d", task.numberOfSeason);
			}
			
			episodeList.setTitle(title);
			episodeList.setIcon(task.client.getIcon());
			
			tabbedPane.addTab(title, episodeList);
			tabbedPane.setTabComponentAt(tabbedPane.indexOfComponent(episodeList), episodeList.getTabComponent());
			
			episodeList.setLoading(true);
		}
		

		@Override
		public void done(PropertyChangeEvent evt) {
			// tab might have been closed
			if (tabbedPane.indexOfComponent(episodeList) < 0)
				return;
			
			SearchTask task = (SearchTask) evt.getSource();
			
			Collection<SearchResult> searchResults;
			
			try {
				searchResults = task.get();
			} catch (Exception e) {
				tabbedPane.remove(episodeList);
				
				Throwable cause = ExceptionUtil.getRootCause(e);
				
				MessageManager.showWarning(cause.getMessage());
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING, cause.toString());
				
				return;
			}
			
			SearchResult selectedResult = null;
			
			if (searchResults.size() == 1) {
				// only one show found, select this one
				selectedResult = searchResults.iterator().next();
			} else if (searchResults.size() > 1) {
				// multiple shows found, let user selected one
				Window window = SwingUtilities.getWindowAncestor(SearchPanel.this);
				
				SelectDialog<SearchResult> select = new SelectDialog<SearchResult>(window, searchResults);
				
				select.setText("Select a Show:");
				
				select.setIconImage(TunedUtil.getImage(episodeList.getIcon()));
				select.setVisible(true);
				
				selectedResult = select.getSelectedValue();
			} else {
				MessageManager.showWarning("\"" + task.query + "\" has not been found.");
			}
			
			if (selectedResult == null) {
				tabbedPane.remove(episodeList);
				return;
			}
			
			String title = selectedResult.getName();
			
			//			searchFieldCompletion.addTerm(title);
			//TODO fix
			//			Settings.getSettings().putStringList(Settings.SEARCH_HISTORY, searchFieldCompletion.getTerms());
			
			if (task.numberOfSeason != SeasonSpinnerModel.ALL_SEASONS) {
				title += String.format(" - Season %d", task.numberOfSeason);
			}
			
			episodeList.setTitle(title);
			
			FetchEpisodeListTask getEpisodesTask = new FetchEpisodeListTask(task.client, selectedResult, task.numberOfSeason);
			getEpisodesTask.addPropertyChangeListener(new FetchEpisodeListTaskListener(episodeList));
			
			getEpisodesTask.execute();
		}
	}
	

	private class FetchEpisodeListTaskListener extends SwingWorkerPropertyChangeAdapter {
		
		private EpisodeListPanel episodeList;
		
		
		public FetchEpisodeListTaskListener(EpisodeListPanel episodeList) {
			this.episodeList = episodeList;
		}
		

		@Override
		public void done(PropertyChangeEvent evt) {
			// tab might have been closed
			if (tabbedPane.indexOfComponent(episodeList) < 0)
				return;
			
			FetchEpisodeListTask task = (FetchEpisodeListTask) evt.getSource();
			
			try {
				URI link = task.getSearchEngine().getEpisodeListLink(task.getSearchResult(), task.getNumberOfSeason());
				
				Collection<Episode> episodes = task.get();
				
				String info = (episodes.size() > 0) ? String.format("%d episodes", episodes.size()) : "No episodes found";
				
				historyPanel.add(episodeList.getTitle(), link, episodeList.getIcon(), info, NumberFormat.getInstance().format(task.getDuration()) + " ms");
				
				if (episodes.size() <= 0)
					tabbedPane.remove(episodeList);
				else {
					episodeList.setLoading(false);
					episodeList.getModel().addAll(episodes);
				}
			} catch (Exception e) {
				tabbedPane.remove(episodeList);
				
				Throwable cause = ExceptionUtil.getRootCause(e);
				
				MessageManager.showWarning(cause.getMessage());
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, cause.getMessage(), cause);
			}
		}
	}
	
}
