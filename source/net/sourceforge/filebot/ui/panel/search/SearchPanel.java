
package net.sourceforge.filebot.ui.panel.search;


import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.filebot.ui.FileBotList;
import net.sourceforge.filebot.ui.FileBotPanel;
import net.sourceforge.filebot.ui.FileBotUtil;
import net.sourceforge.filebot.ui.MessageManager;
import net.sourceforge.filebot.ui.transfer.SaveAction;
import net.sourceforge.filebot.web.AnidbClient;
import net.sourceforge.filebot.web.Episode;
import net.sourceforge.filebot.web.EpisodeListClient;
import net.sourceforge.filebot.web.TVRageClient;
import net.sourceforge.filebot.web.TvdotcomClient;
import net.sourceforge.tuned.ui.SelectButton;
import net.sourceforge.tuned.ui.SelectDialog;
import net.sourceforge.tuned.ui.SwingWorkerPropertyChangeAdapter;
import net.sourceforge.tuned.ui.TextCompletion;
import net.sourceforge.tuned.ui.TextFieldWithSelect;


public class SearchPanel extends FileBotPanel {
	
	private JTabbedPane tabbedPane = new JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
	
	private HistoryPanel history = new HistoryPanel();
	
	private SpinnerNumberModel seasonSpinnerModel = new SpinnerNumberModel(SeasonSpinnerEditor.ALL_SEASONS, SeasonSpinnerEditor.ALL_SEASONS, Integer.MAX_VALUE, 1);
	
	private TextFieldWithSelect<EpisodeListClient> searchField;
	
	private TextCompletion searchFieldCompletion;
	
	private List<EpisodeListClient> episodeListClients = new ArrayList<EpisodeListClient>();
	
	private JSpinner seasonSpinner;
	
	
	public SearchPanel() {
		super("Search", ResourceManager.getIcon("panel.search"));
		
		episodeListClients.add(new TvdotcomClient());
		episodeListClients.add(new AnidbClient());
		episodeListClients.add(new TVRageClient());
		
		HashMap<EpisodeListClient, ImageIcon> icons = new HashMap<EpisodeListClient, ImageIcon>();
		
		for (EpisodeListClient searchEngine : episodeListClients) {
			icons.put(searchEngine, searchEngine.getIcon());
		}
		
		searchField = new TextFieldWithSelect<EpisodeListClient>(episodeListClients, icons);
		searchField.getSelectButton().addPropertyChangeListener(SelectButton.SELECTED_VALUE_PROPERTY, searchFieldListener);
		searchField.getTextField().setColumns(25);
		
		searchFieldCompletion = new TextCompletion(searchField.getTextField());
		searchFieldCompletion.hook();
		
		JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
		
		Box searchBox = Box.createHorizontalBox();
		searchBox.setBorder(new EmptyBorder(5, 5, 5, 5));
		
		seasonSpinner = new JSpinner(seasonSpinnerModel);
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
		
		JScrollPane sp = new JScrollPane(history);
		sp.setBorder(BorderFactory.createEmptyBorder());
		tabbedPane.addTab("History", ResourceManager.getIcon("tab.history"), sp);
		
		mainPanel.add(searchBox, BorderLayout.NORTH);
		mainPanel.add(centerPanel, BorderLayout.CENTER);
		
		this.add(mainPanel, BorderLayout.CENTER);
		
		FileBotUtil.registerActionForKeystroke(this, KeyStroke.getKeyStroke("ENTER"), searchAction);
		FileBotUtil.registerActionForKeystroke(this, KeyStroke.getKeyStroke("UP"), upAction);
		FileBotUtil.registerActionForKeystroke(this, KeyStroke.getKeyStroke("DOWN"), downAction);
		FileBotUtil.registerActionForKeystroke(this, KeyStroke.getKeyStroke("shift UP"), new SpinSearchEngineAction(-1));
		FileBotUtil.registerActionForKeystroke(this, KeyStroke.getKeyStroke("shift DOWN"), new SpinSearchEngineAction(1));
	}
	

	public void setSeasonValue(Object value) {
		if (value != null)
			seasonSpinnerModel.setValue(value);
	}
	
	private final PropertyChangeListener searchFieldListener = new PropertyChangeListener() {
		
		public void propertyChange(PropertyChangeEvent evt) {
			EpisodeListClient se = searchField.getSelectedValue();
			
			if (!se.isSingleSeasonSupported()) {
				seasonSpinnerModel.setMaximum(SeasonSpinnerEditor.ALL_SEASONS);
				seasonSpinnerModel.setValue(SeasonSpinnerEditor.ALL_SEASONS);
			} else {
				seasonSpinnerModel.setMaximum(Integer.MAX_VALUE);
			}
			
		}
		
	};
	
	private final AbstractAction searchAction = new AbstractAction("Find", ResourceManager.getIcon("action.find")) {
		
		public void actionPerformed(ActionEvent e) {
			searchField.clearTextSelection();
			
			EpisodeListClient searchEngine = searchField.getSelectedValue();
			SearchTask task = new SearchTask(searchEngine, searchField.getTextField().getText(), seasonSpinnerModel.getNumber().intValue());
			task.addPropertyChangeListener(new SearchTaskListener());
			
			task.execute();
		}
	};
	
	private final AbstractAction upAction = new AbstractAction("Up") {
		
		public void actionPerformed(ActionEvent e) {
			setSeasonValue(seasonSpinnerModel.getNextValue());
		}
	};
	
	private final AbstractAction downAction = new AbstractAction("Down") {
		
		public void actionPerformed(ActionEvent e) {
			setSeasonValue(seasonSpinnerModel.getPreviousValue());
		}
	};
	
	private final SaveAction saveAction = new SaveAction(null) {
		
		private FileBotList current;
		
		
		@Override
		public void actionPerformed(ActionEvent e) {
			Component c = tabbedPane.getSelectedComponent();
			
			if (c instanceof FileBotList) {
				current = (FileBotList) c;
				super.actionPerformed(e);
			}
		}
		

		@Override
		protected boolean isSaveable() {
			return current.isSaveable();
		}
		

		@Override
		protected String getDefaultFileName() {
			return current.getDefaultFileName();
		}
		

		@Override
		protected void save(File file) {
			current.save(file);
			current = null;
		}
	};
	
	
	private class SpinSearchEngineAction extends AbstractAction {
		
		private int spinOffset;
		
		
		public SpinSearchEngineAction(int spinOffset) {
			super("Spin Search Engine");
			this.spinOffset = spinOffset;
		}
		

		public void actionPerformed(ActionEvent e) {
			EpisodeListClient current = searchField.getSelectedValue();
			
			int nextIndex = episodeListClients.indexOf(current) + spinOffset;
			int maxIndex = episodeListClients.size() - 1;
			
			if (nextIndex < 0)
				nextIndex = maxIndex;
			else if (nextIndex > maxIndex)
				nextIndex = 0;
			
			searchField.getSelectButton().setSelectedValue(episodeListClients.get(nextIndex));
		}
	}
	

	private class SearchTask extends SwingWorker<List<String>, Object> {
		
		private String searchTerm;
		private EpisodeListClient searchEngine;
		private int numberOfSeason;
		
		
		public SearchTask(EpisodeListClient searchEngine, String searchterm, int numberOfSeason) {
			searchTerm = searchterm;
			this.searchEngine = searchEngine;
			this.numberOfSeason = numberOfSeason;
		}
		

		public String getSearchTerm() {
			return searchTerm;
		}
		

		public int getNumberOfSeason() {
			return numberOfSeason;
		}
		

		public EpisodeListClient getSearchEngine() {
			return searchEngine;
		}
		

		@Override
		protected List<String> doInBackground() throws Exception {
			return searchEngine.search(searchTerm);
		}
		
	}
	

	private class SearchTaskListener extends SwingWorkerPropertyChangeAdapter {
		
		private EpisodeListPanel episodeList;
		
		
		@Override
		public void started(PropertyChangeEvent evt) {
			SearchTask task = (SearchTask) evt.getSource();
			
			episodeList = new EpisodeListPanel();
			
			String title = task.getSearchTerm();
			
			if (task.getNumberOfSeason() != SeasonSpinnerEditor.ALL_SEASONS)
				title += " - Season " + task.getNumberOfSeason();
			
			episodeList.setTitle(title);
			episodeList.setIcon(task.getSearchEngine().getIcon());
			
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
			
			List<String> shows = null;
			
			try {
				shows = task.get();
			} catch (Exception e) {
				tabbedPane.remove(episodeList);
				
				Throwable t = e;
				
				while (t.getCause() != null) {
					t = t.getCause();
				}
				
				MessageManager.showWarning(t.toString());
				t.printStackTrace();
				
				return;
			}
			
			String showname = null;
			
			if (shows.size() == 1)
				showname = shows.get(0);
			else if (shows.size() > 1) {
				Window window = SwingUtilities.getWindowAncestor(SearchPanel.this);
				
				SelectDialog<String> select = new SelectDialog<String>(window, shows);
				
				select.setText("Select a Show:");
				select.setIconImage(episodeList.getIcon().getImage());
				select.setVisible(true);
				showname = select.getSelectedValue();
			} else
				MessageManager.showWarning("\"" + task.getSearchTerm() + "\" has not been found.");
			
			if (showname == null) {
				tabbedPane.remove(episodeList);
				return;
			}
			
			searchFieldCompletion.addCompletionTerm(showname);
			
			String title = showname;
			if (task.getNumberOfSeason() != SeasonSpinnerEditor.ALL_SEASONS)
				title += " - Season " + task.getNumberOfSeason();
			
			episodeList.setTitle(title);
			
			FetchEpisodesTask getEpisodesTask = new FetchEpisodesTask(task.getSearchEngine(), showname, task.getNumberOfSeason());
			getEpisodesTask.addPropertyChangeListener(new FetchEpisodesTaskListener(episodeList));
			getEpisodesTask.execute();
		}
	}
	

	private class FetchEpisodesTaskListener extends SwingWorkerPropertyChangeAdapter {
		
		private EpisodeListPanel episodeList;
		
		
		public FetchEpisodesTaskListener(EpisodeListPanel episodeList) {
			this.episodeList = episodeList;
		}
		

		@Override
		public void done(PropertyChangeEvent evt) {
			// tab might have been closed
			if (tabbedPane.indexOfComponent(episodeList) < 0)
				return;
			
			FetchEpisodesTask task = (FetchEpisodesTask) evt.getSource();
			
			try {
				URL url = task.getSearchEngine().getEpisodeListUrl(task.getShowName(), task.getNumberOfSeason());
				
				Collection<Episode> episodes = task.get();
				
				history.add(episodeList.getTitle(), url, episodes.size(), task.getDuration(), episodeList.getIcon());
				
				if (episodes.size() <= 0)
					tabbedPane.remove(episodeList);
				else {
					episodeList.setLoading(false);
					
					DefaultListModel model = episodeList.getModel();
					
					for (Episode e : episodes)
						model.addElement(e.toString());
				}
			} catch (Exception e) {
				tabbedPane.remove(episodeList);
				
				Throwable t = e;
				
				// find root cause
				
				while (t.getCause() != null) {
					t = t.getCause();
				}
				
				MessageManager.showWarning(t.toString());
				t.printStackTrace();
			}
		}
		
	}
	
}
