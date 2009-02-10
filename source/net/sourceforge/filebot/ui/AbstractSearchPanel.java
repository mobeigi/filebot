
package net.sourceforge.filebot.ui;


import static javax.swing.JTabbedPane.SCROLL_TAB_LAYOUT;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;
import static javax.swing.SwingConstants.TOP;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.net.URI;
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

import net.miginfocom.swing.MigLayout;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.web.SearchResult;
import net.sourceforge.tuned.ExceptionUtilities;
import net.sourceforge.tuned.ui.LabelProvider;
import net.sourceforge.tuned.ui.SelectButtonTextField;
import net.sourceforge.tuned.ui.TunedUtilities;
import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.AutoCompleteSupport;


public abstract class AbstractSearchPanel<S, E> extends FileBotPanel {
	
	protected final JPanel tabbedPaneGroup = new JPanel(new MigLayout("nogrid, fill, insets 0"));
	
	protected final JTabbedPane tabbedPane = new JTabbedPane(TOP, SCROLL_TAB_LAYOUT);
	
	protected final HistoryPanel historyPanel = new HistoryPanel();
	
	protected final SelectButtonTextField<S> searchTextField = new SelectButtonTextField<S>();
	
	private EventList<String> searchHistory = new BasicEventList<String>();
	
	
	public AbstractSearchPanel(String title, Icon icon) {
		super(title, icon);
		
		historyPanel.setColumnHeader(2, "Duration");
		
		JScrollPane historyScrollPane = new JScrollPane(historyPanel, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER);
		historyScrollPane.setBorder(BorderFactory.createEmptyBorder());
		
		tabbedPane.addTab("History", ResourceManager.getIcon("tab.history"), historyScrollPane);
		
		tabbedPaneGroup.setBorder(BorderFactory.createTitledBorder("Search Results"));
		tabbedPaneGroup.add(tabbedPane, "grow, wrap 8px");
		
		setLayout(new MigLayout("nogrid, fill, insets 10px 10px 15px 10px"));
		add(searchTextField, "alignx center, gapafter indent");
		add(new JButton(searchAction), "gap 18px, wrap 10px");
		add(tabbedPaneGroup, "grow");
		
		/*
		 * TODO: fetchHistory
		// no need to care about thread-safety, history-lists are only accessed from the EDT
		CompositeList<Object> completionList = new CompositeList<Object>();
		
		completionList.addMemberList((EventList) searchHistory);
		completionList.addMemberList(fetchHistory);
		*/

		searchTextField.getEditor().setAction(searchAction);
		
		searchTextField.getSelectButton().setModel(createSearchEngines());
		searchTextField.getSelectButton().setLabelProvider(createSearchEngineLabelProvider());
		
		AutoCompleteSupport.install(searchTextField.getEditor(), searchHistory);
		
		TunedUtilities.putActionForKeystroke(this, KeyStroke.getKeyStroke("ENTER"), searchAction);
	}
	

	protected abstract List<S> createSearchEngines();
	

	protected abstract LabelProvider<S> createSearchEngineLabelProvider();
	

	protected abstract RequestProcessor<?, E> createRequestProcessor();
	

	public EventList<String> getSearchHistory() {
		return searchHistory;
	}
	

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
				// choose search result
				requestProcessor.setSearchResult(requestProcessor.chooseSearchResult(get(), SwingUtilities.getWindowAncestor(AbstractSearchPanel.this)));
				
				if (requestProcessor.getSearchResult() == null) {
					tab.close();
					return;
				}
				
				String title = requestProcessor.getTitle();
				
				if (!searchHistory.contains(title)) {
					searchHistory.add(title);
				}
				
				tab.setTitle(title);
				
				// fetch elements of the selected search result
				new FetchTask(requestProcessor).execute();
			} catch (Exception e) {
				tab.close();
				
				Logger.getLogger("ui").log(Level.WARNING, ExceptionUtilities.getRootCauseMessage(e), e);
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
				// check if exception occurred
				Collection<E> elements = get();
				
				requestProcessor.process(elements);
				
				String title = requestProcessor.getTitle();
				Icon icon = requestProcessor.getIcon();
				String statusMessage = requestProcessor.getStatusMessage(elements);
				
				historyPanel.add(title, requestProcessor.getLink(), icon, statusMessage, String.format("%,d ms", requestProcessor.getDuration()));
				
				// close tab if no elements were fetched
				if (get().size() <= 0) {
					Logger.getLogger("ui").warning(statusMessage);
					tab.close();
				}
			} catch (Exception e) {
				tab.close();
				
				Logger.getLogger("ui").log(Level.WARNING, ExceptionUtilities.getRootCauseMessage(e), e);
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
		

		public Icon getIcon() {
			return null;
		}
		

		protected SearchResult chooseSearchResult(Collection<? extends SearchResult> searchResults, Window window) throws Exception {
			
			switch (searchResults.size()) {
				case 0:
					Logger.getLogger("ui").warning(String.format("'%s' has not been found.", request.getSearchText()));
					return null;
				case 1:
					return searchResults.iterator().next();
			}
			
			// check if an exact match has been found
			for (SearchResult searchResult : searchResults) {
				if (request.getSearchText().equalsIgnoreCase(searchResult.getName()))
					return searchResult;
			}
			
			// multiple results have been found, user must select one
			SelectDialog<SearchResult> selectDialog = new SelectDialog<SearchResult>(window, searchResults);
			
			configureSelectDialog(selectDialog);
			
			selectDialog.setVisible(true);
			
			// selected value or null if the dialog was canceled by the user
			return selectDialog.getSelectedValue();
		}
		

		protected void configureSelectDialog(SelectDialog<SearchResult> selectDialog) {
			selectDialog.setIconImage(TunedUtilities.getImage(getIcon()));
		}
		

		public long getDuration() {
			return duration;
		}
		
	}
	
}
