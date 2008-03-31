
package net.sourceforge.filebot.ui.panel.subtitle;


import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

import net.sourceforge.filebot.FileBotUtil;
import net.sourceforge.filebot.Settings;
import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.filebot.ui.FileBotPanel;
import net.sourceforge.filebot.ui.FileBotTabComponent;
import net.sourceforge.filebot.ui.HistoryPanel;
import net.sourceforge.filebot.ui.MessageManager;
import net.sourceforge.filebot.ui.SelectDialog;
import net.sourceforge.filebot.web.MovieDescriptor;
import net.sourceforge.filebot.web.SubtitleClient;
import net.sourceforge.filebot.web.SubtitleDescriptor;
import net.sourceforge.tuned.ui.SelectButton;
import net.sourceforge.tuned.ui.SwingWorkerPropertyChangeAdapter;
import net.sourceforge.tuned.ui.TextCompletion;
import net.sourceforge.tuned.ui.TextFieldWithSelect;


public class SubtitlePanel extends FileBotPanel {
	
	private JTabbedPane tabbedPane = new JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
	
	private HistoryPanel historyPanel = new HistoryPanel("Show / Movie", "Number of Subtitles");
	
	private TextFieldWithSelect<SubtitleClient> searchField;
	
	private TextCompletion searchFieldCompletion;
	
	
	public SubtitlePanel() {
		super("Subtitle", ResourceManager.getIcon("panel.subtitle"));
		
		List<SelectButton.Entry<SubtitleClient>> clients = new ArrayList<SelectButton.Entry<SubtitleClient>>();
		
		for (SubtitleClient client : SubtitleClient.getAvailableSubtitleClients()) {
			clients.add(new SelectButton.Entry<SubtitleClient>(client, client.getIcon()));
		}
		
		searchField = new TextFieldWithSelect<SubtitleClient>(clients);
		searchField.getTextField().setColumns(25);
		
		searchFieldCompletion = new TextCompletion(searchField.getTextField());
		searchFieldCompletion.addTerms(Settings.getSettings().getStringList(Settings.SUBTITLE_HISTORY));
		searchFieldCompletion.hook();
		
		JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
		
		Box searchBox = Box.createHorizontalBox();
		searchBox.setBorder(new EmptyBorder(5, 5, 5, 5));
		
		searchField.setMaximumSize(searchField.getPreferredSize());
		
		searchBox.add(Box.createHorizontalGlue());
		searchBox.add(searchField);
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
		
		tabbedPane.addTab("History", ResourceManager.getIcon("tab.history"), historyPanel);
		
		mainPanel.add(searchBox, BorderLayout.NORTH);
		mainPanel.add(centerPanel, BorderLayout.CENTER);
		
		this.add(mainPanel, BorderLayout.CENTER);
		
		FileBotUtil.registerActionForKeystroke(this, KeyStroke.getKeyStroke("ENTER"), searchAction);
		FileBotUtil.registerActionForKeystroke(this, KeyStroke.getKeyStroke("shift UP"), new SpinClientAction(-1));
		FileBotUtil.registerActionForKeystroke(this, KeyStroke.getKeyStroke("shift DOWN"), new SpinClientAction(1));
	}
	
	
	private class SpinClientAction extends AbstractAction {
		
		private int spin;
		
		
		public SpinClientAction(int spin) {
			this.spin = spin;
		}
		

		public void actionPerformed(ActionEvent e) {
			searchField.getSelectButton().spinValue(spin);
		}
	}
	
	private final AbstractAction searchAction = new AbstractAction("Find", ResourceManager.getIcon("action.find")) {
		
		public void actionPerformed(ActionEvent e) {
			searchField.clearTextSelection();
			
			SearchTask searchTask = new SearchTask(searchField.getSelectedValue(), searchField.getTextField().getText());
			searchTask.addPropertyChangeListener(new SearchTaskListener());
			
			searchTask.execute();
		}
	};
	
	private final AbstractAction saveAction = new AbstractAction("Down") {
		
		public void actionPerformed(ActionEvent e) {
			//TODO save action
		}
	};
	
	
	private class SearchTask extends SwingWorker<List<MovieDescriptor>, Object> {
		
		private final String query;
		private final SubtitleClient client;
		
		
		public SearchTask(SubtitleClient client, String query) {
			this.client = client;
			this.query = query;
		}
		

		@Override
		protected List<MovieDescriptor> doInBackground() throws Exception {
			return client.search(query);
		}
		
	}
	

	private class SearchTaskListener extends SwingWorkerPropertyChangeAdapter {
		
		private SubtitleListPanel subtitleSearchResultPanel;
		private FileBotTabComponent tabComponent;
		
		
		@Override
		public void started(PropertyChangeEvent evt) {
			SearchTask task = (SearchTask) evt.getSource();
			
			subtitleSearchResultPanel = new SubtitleListPanel();
			tabComponent = new FileBotTabComponent(task.query, ResourceManager.getIcon("tab.loading"));
			
			tabbedPane.addTab(task.query, subtitleSearchResultPanel);
			tabbedPane.setTabComponentAt(tabbedPane.indexOfComponent(subtitleSearchResultPanel), tabComponent);
		}
		

		@Override
		public void done(PropertyChangeEvent evt) {
			// tab might have been closed
			if (tabbedPane.indexOfComponent(subtitleSearchResultPanel) < 0)
				return;
			
			SearchTask searchTask = (SearchTask) evt.getSource();
			
			try {
				List<MovieDescriptor> desriptors = searchTask.get();
				
				MovieDescriptor descriptor = selectDescriptor(desriptors, searchTask.client);
				
				if (descriptor == null) {
					if (desriptors.isEmpty()) {
						MessageManager.showWarning(String.format("\"%s\" has not been found.", searchTask.query));
					}
					
					tabbedPane.remove(subtitleSearchResultPanel);
					return;
				}
				
				fetchSubtitles(descriptor, searchTask.client);
				
			} catch (Exception e) {
				tabbedPane.remove(subtitleSearchResultPanel);
				
				Throwable cause = FileBotUtil.getRootCause(e);
				
				MessageManager.showWarning(cause.getMessage());
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING, cause.toString());
			}
			
		}
		

		private MovieDescriptor selectDescriptor(List<MovieDescriptor> descriptors, SubtitleClient client) {
			switch (descriptors.size()) {
				case 0:
					return null;
				case 1:
					return descriptors.get(0);
			}
			
			// multiple shows found, let user selected one
			Window window = SwingUtilities.getWindowAncestor(SubtitlePanel.this);
			
			SelectDialog<MovieDescriptor> selectDialog = new SelectDialog<MovieDescriptor>(window, descriptors);
			
			selectDialog.setText("Select a Show / Movie:");
			selectDialog.setIconImage(client.getIcon().getImage());
			selectDialog.setVisible(true);
			
			// selected value or null if cancelled by the user
			return selectDialog.getSelectedValue();
		}
		

		private void fetchSubtitles(MovieDescriptor descriptor, SubtitleClient client) {
			
			Settings.getSettings().putStringList(Settings.SUBTITLE_HISTORY, searchFieldCompletion.getTerms());
			searchFieldCompletion.addTerm(descriptor.getTitle());
			
			tabComponent.setText(descriptor.getTitle());
			
			FetchSubtitleListTask fetchListTask = new FetchSubtitleListTask(descriptor, client);
			fetchListTask.addPropertyChangeListener(new FetchSubtitleListTaskListener(subtitleSearchResultPanel, tabComponent));
			
			fetchListTask.execute();
		}
	}
	

	private class FetchSubtitleListTask extends SwingWorker<List<? extends SubtitleDescriptor>, Object> {
		
		private final SubtitleClient client;
		private final MovieDescriptor descriptor;
		
		
		public FetchSubtitleListTask(MovieDescriptor descriptor, SubtitleClient client) {
			this.descriptor = descriptor;
			this.client = client;
		}
		

		@Override
		protected List<? extends SubtitleDescriptor> doInBackground() throws Exception {
			return client.getSubtitleList(descriptor);
		}
	}
	

	private class FetchSubtitleListTaskListener extends SwingWorkerPropertyChangeAdapter {
		
		private final SubtitleListPanel subtitleSearchResultPanel;
		private final FileBotTabComponent tabComponent;
		
		
		public FetchSubtitleListTaskListener(SubtitleListPanel subtitleSearchResultPanel, FileBotTabComponent tabComponent) {
			this.subtitleSearchResultPanel = subtitleSearchResultPanel;
			this.tabComponent = tabComponent;
		}
		

		@Override
		public void done(PropertyChangeEvent evt) {
			// tab might have been closed
			if (tabbedPane.indexOfComponent(subtitleSearchResultPanel) < 0)
				return;
			
			FetchSubtitleListTask task = (FetchSubtitleListTask) evt.getSource();
			
			try {
				List<? extends SubtitleDescriptor> subtitleDescriptors = task.get();
				
				String info = (subtitleDescriptors.size() > 0) ? String.format("%d subtitles", subtitleDescriptors.size()) : "No subtitles found";
				
				historyPanel.add(task.descriptor.toString(), null, info, 0, task.client.getIcon());
				
				if (subtitleDescriptors.isEmpty()) {
					tabbedPane.remove(subtitleSearchResultPanel);
					return;
				}
				
				tabComponent.setIcon(task.client.getIcon());
				
				//TODO icon view
				//TODO sysout
				System.out.println(subtitleDescriptors);
			} catch (Exception e) {
				tabbedPane.remove(subtitleSearchResultPanel);
				
				MessageManager.showWarning(FileBotUtil.getRootCause(e).getMessage());
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, e.toString(), e);
			}
		}
	}
	
}
