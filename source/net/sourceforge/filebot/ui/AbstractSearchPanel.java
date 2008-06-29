
package net.sourceforge.filebot.ui;


import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.net.URI;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.filebot.web.SearchResult;
import net.sourceforge.tuned.ExceptionUtil;
import net.sourceforge.tuned.ProgressIterator;
import net.sourceforge.tuned.ui.SelectButtonTextField;
import net.sourceforge.tuned.ui.SwingWorkerPropertyChangeAdapter;
import net.sourceforge.tuned.ui.TunedUtil;
import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.AutoCompleteSupport;


public abstract class AbstractSearchPanel<S, E, T extends JComponent> extends FileBotPanel {
	
	private final JTabbedPane tabbedPane = new JTabbedPane(SwingConstants.TOP, JTabbedPane.WRAP_TAB_LAYOUT);
	
	private final HistoryPanel historyPanel = new HistoryPanel();
	
	private final SelectButtonTextField<S> searchField;
	
	private final EventList<String> searchHistory = new BasicEventList<String>();
	
	
	@SuppressWarnings("unchecked")
	public AbstractSearchPanel(String title, Icon icon) {
		super(title, icon);
		
		setLayout(new BorderLayout(10, 5));
		
		searchField = new SelectButtonTextField<S>();
		
		Box searchBox = Box.createHorizontalBox();
		searchBox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		
		searchField.setMaximumSize(searchField.getPreferredSize());
		
		searchBox.add(Box.createHorizontalGlue());
		searchBox.add(searchField);
		searchBox.add(Box.createHorizontalStrut(15));
		searchBox.add(new JButton(searchAction));
		searchBox.add(Box.createHorizontalGlue());
		
		JPanel centerPanel = new JPanel(new BorderLayout());
		centerPanel.setBorder(BorderFactory.createTitledBorder("Search Results"));
		
		centerPanel.add(tabbedPane, BorderLayout.CENTER);
		
		historyPanel.setColumnHeader3("Duration");
		
		JScrollPane historyScrollPane = new JScrollPane(historyPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		historyScrollPane.setBorder(BorderFactory.createEmptyBorder());
		
		tabbedPane.addTab("History", ResourceManager.getIcon("tab.history"), historyScrollPane);
		
		add(searchBox, BorderLayout.NORTH);
		add(centerPanel, BorderLayout.CENTER);
		
		/*
		 * TODO: fetchHistory
		// no need to care about thread-safety, history-lists are only accessed from the EDT
		CompositeList<Object> completionList = new CompositeList<Object>();
		
		completionList.addMemberList((EventList) searchHistory);
		completionList.addMemberList(fetchHistory);
		*/

		AutoCompleteSupport.install(searchField.getEditor(), searchHistory);
		
		TunedUtil.registerActionForKeystroke(this, KeyStroke.getKeyStroke("ENTER"), searchAction);
	}
	

	protected abstract SearchTask createSearchTask();
	

	protected abstract void configureSelectDialog(SelectDialog<SearchResult> selectDialog);
	

	protected abstract FetchTask createFetchTask(SearchTask searchTask, SearchResult selectedSearchResult);
	

	protected abstract URI getLink(S client, SearchResult searchResult);
	

	public EventList<String> getSearchHistory() {
		return searchHistory;
	}
	

	public HistoryPanel getHistoryPanel() {
		return historyPanel;
	}
	

	public SelectButtonTextField<S> getSearchField() {
		return searchField;
	}
	
	private final AbstractAction searchAction = new AbstractAction("Find", ResourceManager.getIcon("action.find")) {
		
		public void actionPerformed(ActionEvent e) {
			
			SearchTask searchTask = createSearchTask();
			searchTask.addPropertyChangeListener(new SearchTaskListener());
			
			searchTask.execute();
		}
	};
	
	
	protected abstract class SearchTask extends SwingWorker<List<SearchResult>, Void> {
		
		private final String searchText;
		private final S client;
		
		private final T tabPanel;
		
		
		public SearchTask(S client, String searchText, T tabPanel) {
			this.searchText = searchText;
			this.client = client;
			this.tabPanel = tabPanel;
		}
		

		@Override
		protected abstract List<SearchResult> doInBackground() throws Exception;
		

		public String getSearchText() {
			return searchText;
		}
		

		public S getClient() {
			return client;
		}
		

		public T getTabPanel() {
			return tabPanel;
		}
		
	}
	

	private class SearchTaskListener extends SwingWorkerPropertyChangeAdapter {
		
		private FileBotTab<T> tab;
		
		
		@Override
		public void started(PropertyChangeEvent evt) {
			SearchTask task = (SearchTask) evt.getSource();
			
			tab = new FileBotTab<T>(task.getTabPanel());
			
			tab.setTitle(task.getSearchText());
			tab.setLoading(true);
			tab.setIcon(searchField.getSelectButton().getIconProvider().getIcon(task.getClient()));
			
			tab.addTo(tabbedPane);
			
			tabbedPane.setSelectedComponent(tab);
		}
		

		@Override
		public void done(PropertyChangeEvent evt) {
			// tab might have been closed
			if (tab.isClosed())
				return;
			
			SearchTask task = (SearchTask) evt.getSource();
			
			try {
				SearchResult selectedResult = selectSearchResult(task);
				
				if (selectedResult == null) {
					if (task.get().isEmpty()) {
						// no search results
						MessageManager.showWarning(String.format("\"%s\" has not been found.", task.getSearchText()));
					}
					
					tab.close();
					return;
				}
				
				String title = selectedResult.getName();
				
				if (!searchHistory.contains(title)) {
					searchHistory.add(title);
				}
				
				tab.setTitle(title);
				
				FetchTask fetchTask = createFetchTask(task, selectedResult);
				fetchTask.addPropertyChangeListener(new FetchTaskListener(tab));
				
				fetchTask.execute();
			} catch (Exception e) {
				tab.close();
				
				Throwable cause = ExceptionUtil.getRootCause(e);
				
				MessageManager.showWarning(cause.getMessage());
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, cause.getMessage(), cause);
			}
			
		}
		

		private SearchResult selectSearchResult(SearchTask task) throws Exception {
			List<SearchResult> searchResults = task.get();
			
			switch (searchResults.size()) {
				case 0:
					return null;
				case 1:
					return searchResults.get(0);
			}
			
			// multiple results have been found, user must selected one
			Window window = SwingUtilities.getWindowAncestor(AbstractSearchPanel.this);
			
			SelectDialog<SearchResult> selectDialog = new SelectDialog<SearchResult>(window, searchResults);
			
			selectDialog.setIconImage(TunedUtil.getImage(searchField.getSelectButton().getIconProvider().getIcon(task.getClient())));
			
			configureSelectDialog(selectDialog);
			selectDialog.setVisible(true);
			
			// selected value or null if canceled by the user
			return selectDialog.getSelectedValue();
		}
		
	}
	

	protected abstract class FetchTask extends SwingWorker<Void, E> {
		
		private long duration = -1;
		private int count = 0;
		
		private final S client;
		private final SearchResult searchResult;
		private final T tabPanel;
		
		
		public FetchTask(S client, SearchResult searchResult, T tabPanel) {
			this.client = client;
			this.searchResult = searchResult;
			this.tabPanel = tabPanel;
		}
		

		@SuppressWarnings("unchecked")
		@Override
		protected final Void doInBackground() throws Exception {
			long start = System.currentTimeMillis();
			
			ProgressIterator<E> iterator = fetch();
			
			while (!isCancelled() && iterator.hasNext()) {
				
				try {
					publish(iterator.next());
				} catch (Exception e) {
					Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING, e.getMessage());
				}
				
				setProgress((iterator.getPosition() * 100) / iterator.getLength());
				count++;
			}
			
			duration = System.currentTimeMillis() - start;
			
			return null;
		}
		

		protected abstract ProgressIterator<E> fetch() throws Exception;
		

		@Override
		protected abstract void process(List<E> elements);
		

		public abstract String getStatusMessage();
		

		public S getClient() {
			return client;
		}
		

		public SearchResult getSearchResult() {
			return searchResult;
		}
		

		public T getTabPanel() {
			return tabPanel;
		}
		

		public long getDuration() {
			return duration;
		}
		

		public int getCount() {
			return count;
		}
		
	}
	

	private class FetchTaskListener extends SwingWorkerPropertyChangeAdapter {
		
		private final FileBotTab<T> tab;
		
		private CancelAction cancelOnClose;
		
		
		public FetchTaskListener(FileBotTab<T> tab) {
			this.tab = tab;
		}
		

		@Override
		public void started(PropertyChangeEvent evt) {
			cancelOnClose = new CancelAction((SwingWorker<?, ?>) evt.getSource());
			tab.getTabComponent().getCloseButton().addActionListener(cancelOnClose);
		}
		

		@Override
		public void done(PropertyChangeEvent evt) {
			tab.getTabComponent().getCloseButton().removeActionListener(cancelOnClose);
			
			FetchTask task = (FetchTask) evt.getSource();
			
			// tab might still be open, even if task was cancelled
			if (tab.isClosed() || task.isCancelled())
				return;
			
			try {
				// check if exception occurred
				task.get();
				
				String title = task.getSearchResult().toString();
				URI link = getLink(task.getClient(), task.getSearchResult());
				Icon icon = searchField.getSelectButton().getIconProvider().getIcon(task.getClient());
				String info = task.getStatusMessage();
				String duration = String.format("%,d ms", task.getDuration());
				
				historyPanel.add(title, link, icon, info, duration);
				
				// close tab if no elements were fetched
				if (task.getCount() <= 0) {
					MessageManager.showWarning(info);
					tab.close();
				}
			} catch (Exception e) {
				tab.close();
				
				MessageManager.showWarning(ExceptionUtil.getRootCause(e).getMessage());
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, e.toString(), e);
			}
			
			tab.setLoading(false);
		}
	}
	

	private static class CancelAction implements ActionListener {
		
		private final SwingWorker<?, ?> worker;
		
		
		public CancelAction(SwingWorker<?, ?> worker) {
			this.worker = worker;
		}
		

		@Override
		public void actionPerformed(ActionEvent e) {
			worker.cancel(false);
		}
		
	}
	
}
