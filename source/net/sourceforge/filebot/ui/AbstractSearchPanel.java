
package net.sourceforge.filebot.ui;


import static javax.swing.ScrollPaneConstants.*;
import static net.sourceforge.filebot.ui.NotificationLogging.*;
import static net.sourceforge.tuned.ui.TunedUtilities.*;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.matchers.TextMatcherEditor;
import ca.odell.glazedlists.swing.AutoCompleteSupport;

import net.miginfocom.swing.MigLayout;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.Settings;
import net.sourceforge.filebot.similarity.SeriesNameMatcher;
import net.sourceforge.filebot.web.SearchResult;
import net.sourceforge.tuned.ExceptionUtilities;
import net.sourceforge.tuned.ListChangeSynchronizer;
import net.sourceforge.tuned.ui.LabelProvider;


public abstract class AbstractSearchPanel<S, E> extends JComponent {
	
	protected final JPanel tabbedPaneGroup = new JPanel(new MigLayout("nogrid, fill, insets 0", "align center", "[fill]8px[pref!]4px"));
	
	protected final JTabbedPane tabbedPane = new JTabbedPane();
	
	protected final HistoryPanel historyPanel = new HistoryPanel();
	
	protected final SelectButtonTextField<S> searchTextField = new SelectButtonTextField<S>();
	
	protected final EventList<String> searchHistory = createSearchHistory();
	

	public AbstractSearchPanel() {
		historyPanel.setColumnHeader(2, "Duration");
		
		JScrollPane historyScrollPane = new JScrollPane(historyPanel, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER);
		historyScrollPane.setBorder(BorderFactory.createEmptyBorder());
		
		tabbedPane.addTab("History", ResourceManager.getIcon("action.find"), historyScrollPane);
		
		tabbedPaneGroup.setBorder(BorderFactory.createTitledBorder("Search Results"));
		tabbedPaneGroup.add(tabbedPane, "grow, wrap");
		
		setLayout(new MigLayout("nogrid, fill, insets 10px 10px 15px 10px", "align center", "[pref!]10px[fill]"));
		
		add(searchTextField, "gapafter indent");
		add(new JButton(searchAction), "gap 18px, wrap");
		add(tabbedPaneGroup, "grow");
		
		searchTextField.getEditor().setAction(searchAction);
		
		searchTextField.getSelectButton().setModel(Arrays.asList(getSearchEngines()));
		searchTextField.getSelectButton().setLabelProvider(getSearchEngineLabelProvider());
		
		try {
			// restore selected subtitle client
			searchTextField.getSelectButton().setSelectedIndex(Integer.parseInt(getSettings().get("engine.selected", "0")));
		} catch (Exception e) {
			// log and ignore
			Logger.getLogger(getClass().getName()).log(Level.WARNING, e.getMessage(), e);
		}
		
		// save selected client on change
		searchTextField.getSelectButton().getSelectionModel().addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				getSettings().put("engine.selected", Integer.toString(searchTextField.getSelectButton().getSelectedIndex()));
			}
		});
		
		AutoCompleteSupport.install(searchTextField.getEditor(), searchHistory).setFilterMode(TextMatcherEditor.CONTAINS);
		
		installAction(this, KeyStroke.getKeyStroke("ENTER"), searchAction);
	}
	

	protected abstract S[] getSearchEngines();
	

	protected abstract LabelProvider<S> getSearchEngineLabelProvider();
	

	protected abstract Settings getSettings();
	

	protected abstract RequestProcessor<?, E> createRequestProcessor();
	

	private void search(RequestProcessor<?, E> requestProcessor) {
		FileBotTab<?> tab = requestProcessor.tab;
		
		tab.setTitle(requestProcessor.getTitle());
		tab.setLoading(true);
		tab.setIcon(requestProcessor.getIcon());
		
		tab.addTo(tabbedPane);
		
		tabbedPane.setSelectedComponent(tab);
		
		// search in background
		new SearchTask(requestProcessor).execute();
	}
	

	protected EventList<String> createSearchHistory() {
		// create in-memory history
		BasicEventList<String> history = new BasicEventList<String>();
		
		//  get the preferences node that contains the history entries
		//  and get a StringList that read and writes directly from and to the preferences
		List<String> persistentHistory = getSettings().node("history").asList();
		
		// add history from the preferences to the current in-memory history (for completion)
		history.addAll(persistentHistory);
		
		// perform all insert/add/remove operations on the in-memory history on the preferences node as well 
		ListChangeSynchronizer.syncEventListToList(history, persistentHistory);
		
		return history;
	}
	

	private final AbstractAction searchAction = new AbstractAction("Find", ResourceManager.getIcon("action.find")) {
		
		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand() == null) {
				// command triggered by auto-completion
				return;
			}
			
			search(createRequestProcessor());
		}
	};
	

	private class SearchTask extends SwingWorker<Collection<? extends SearchResult>, Void> {
		
		private final RequestProcessor<?, E> requestProcessor;
		

		public SearchTask(RequestProcessor<?, E> requestProcessor) {
			this.requestProcessor = requestProcessor;
		}
		

		@Override
		protected Collection<? extends SearchResult> doInBackground() throws Exception {
			long start = System.currentTimeMillis();
			
			try {
				return requestProcessor.search();
			} finally {
				requestProcessor.duration += (System.currentTimeMillis() - start);
			}
		}
		

		@Override
		public void done() {
			FileBotTab<?> tab = requestProcessor.tab;
			
			// tab might have been closed
			if (tab.isClosed())
				return;
			
			try {
				Collection<? extends SearchResult> results = get();
				
				SearchResult selectedSearchResult = null;
				
				switch (results.size()) {
					case 0:
						UILogger.log(Level.WARNING, String.format("'%s' has not been found.", requestProcessor.request.getSearchText()));
						break;
					case 1:
						selectedSearchResult = results.iterator().next();
						break;
					default:
						selectedSearchResult = requestProcessor.selectSearchResult(results, SwingUtilities.getWindowAncestor(AbstractSearchPanel.this));
						break;
				}
				
				if (selectedSearchResult == null) {
					tab.close();
					return;
				}
				
				// set search result
				requestProcessor.setSearchResult(selectedSearchResult);
				
				String historyEntry = requestProcessor.getHistoryEntry();
				
				if (historyEntry != null && !searchHistory.contains(historyEntry)) {
					searchHistory.add(historyEntry);
				}
				
				tab.setTitle(requestProcessor.getTitle());
				
				// fetch elements of the selected search result
				new FetchTask(requestProcessor).execute();
			} catch (Exception e) {
				tab.close();
				UILogger.log(Level.WARNING, ExceptionUtilities.getRootCauseMessage(e), e);
			}
			
		}
	}
	

	private class FetchTask extends SwingWorker<Collection<E>, Void> {
		
		private final RequestProcessor<?, E> requestProcessor;
		

		public FetchTask(RequestProcessor<?, E> requestProcessor) {
			this.requestProcessor = requestProcessor;
		}
		

		@Override
		protected final Collection<E> doInBackground() throws Exception {
			long start = System.currentTimeMillis();
			
			try {
				return requestProcessor.fetch();
			} finally {
				requestProcessor.duration += (System.currentTimeMillis() - start);
			}
		}
		

		@Override
		public void done() {
			FileBotTab<?> tab = requestProcessor.tab;
			
			if (tab.isClosed())
				return;
			
			try {
				// check if an exception occurred
				Collection<E> elements = get();
				
				requestProcessor.process(elements);
				
				String title = requestProcessor.getTitle();
				Icon icon = requestProcessor.getIcon();
				String statusMessage = requestProcessor.getStatusMessage(elements);
				
				historyPanel.add(title, requestProcessor.getLink(), icon, statusMessage, String.format("%,d ms", requestProcessor.getDuration()));
				
				// close tab if no elements were fetched
				if (get().size() <= 0) {
					UILogger.warning(statusMessage);
					tab.close();
				}
			} catch (Exception e) {
				tab.close();
				UILogger.log(Level.WARNING, ExceptionUtilities.getRootCauseMessage(e), e);
			} finally {
				tab.setLoading(false);
			}
		}
	}
	

	protected static class Request {
		
		private final String searchText;
		

		public Request(String searchText) {
			this.searchText = searchText;
		}
		

		public String getSearchText() {
			return searchText;
		}
		
	}
	

	protected abstract static class RequestProcessor<R extends Request, E> {
		
		protected final R request;
		
		private FileBotTab<JComponent> tab;
		
		private SearchResult searchResult;
		
		private long duration = 0;
		

		public RequestProcessor(R request, JComponent component) {
			this.request = request;
			this.tab = new FileBotTab<JComponent>(component);
		}
		

		public abstract Collection<? extends SearchResult> search() throws Exception;
		

		public abstract Collection<E> fetch() throws Exception;
		

		public abstract void process(Collection<E> elements);
		

		public abstract URI getLink();
		

		public JComponent getComponent() {
			return tab.getComponent();
		}
		

		public SearchResult getSearchResult() {
			return searchResult;
		}
		

		public void setSearchResult(SearchResult searchResult) {
			this.searchResult = searchResult;
		}
		

		public String getStatusMessage(Collection<E> result) {
			return String.format("%d elements found", result.size());
		}
		

		public String getTitle() {
			if (searchResult != null)
				return searchResult.getName();
			
			return request.getSearchText();
		}
		

		public String getHistoryEntry() {
			SeriesNameMatcher nameMatcher = new SeriesNameMatcher();
			
			// the common word sequence of query and search result 
			// common name will maintain the exact word characters (incl. case) of the first argument
			return nameMatcher.matchByFirstCommonWordSequence(searchResult.getName(), request.getSearchText());
		}
		

		public Icon getIcon() {
			return null;
		}
		

		protected SearchResult selectSearchResult(Collection<? extends SearchResult> searchResults, Window window) throws Exception {
			// multiple results have been found, user must select one
			SelectDialog<SearchResult> selectDialog = new SelectDialog<SearchResult>(window, searchResults);
			configureSelectDialog(selectDialog);
			
			selectDialog.setVisible(true);
			
			// selected value or null if the dialog was canceled by the user
			return selectDialog.getSelectedValue();
		}
		

		protected void configureSelectDialog(SelectDialog<SearchResult> selectDialog) {
			selectDialog.setLocation(getOffsetLocation(selectDialog.getOwner()));
			selectDialog.setIconImage(getImage(getIcon()));
		}
		

		public long getDuration() {
			return duration;
		}
		
	}
	
}
