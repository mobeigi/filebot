
package net.sourceforge.filebot.ui.panel.subtitle;


import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

import net.sourceforge.filebot.FileBotUtil;
import net.sourceforge.filebot.Settings;
import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.filebot.ui.FileBotPanel;
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
import net.sourceforge.tuned.ui.TunedUtil;


public class SubtitlePanel extends FileBotPanel {
	
	private JTabbedPane tabbedPane = new JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
	
	private HistoryPanel historyPanel = new HistoryPanel();
	
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
		
		historyPanel.setColumnHeader1("Show / Movie");
		historyPanel.setColumnHeader2("Number of Subtitles");
		historyPanel.setColumnHeader3("Duration");
		
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
		
		JScrollPane historyScrollPane = new JScrollPane(historyPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		historyScrollPane.setBorder(BorderFactory.createEmptyBorder());
		
		tabbedPane.addTab("History", ResourceManager.getIcon("tab.history"), historyScrollPane);
		
		mainPanel.add(searchBox, BorderLayout.NORTH);
		mainPanel.add(centerPanel, BorderLayout.CENTER);
		
		this.add(mainPanel, BorderLayout.CENTER);
		
		TunedUtil.registerActionForKeystroke(this, KeyStroke.getKeyStroke("ENTER"), searchAction);
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
	
	
	private class SearchTask extends SwingWorker<List<MovieDescriptor>, Void> {
		
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
		
		private FileBotTab<SubtitleDownloadPanel> downloadPanel;
		
		
		@Override
		public void started(PropertyChangeEvent evt) {
			SearchTask task = (SearchTask) evt.getSource();
			
			downloadPanel = new FileBotTab<SubtitleDownloadPanel>(new SubtitleDownloadPanel());
			
			downloadPanel.setTitle(task.query);
			downloadPanel.setLoading(true);
			downloadPanel.setIcon(task.client.getIcon());
			
			downloadPanel.addTo(tabbedPane);
		}
		

		@Override
		public void done(PropertyChangeEvent evt) {
			// tab might have been closed
			if (downloadPanel.isClosed())
				return;
			
			SearchTask searchTask = (SearchTask) evt.getSource();
			
			try {
				List<MovieDescriptor> desriptors = searchTask.get();
				
				MovieDescriptor descriptor = selectDescriptor(desriptors, searchTask.client);
				
				if (descriptor == null) {
					// user canceled selection, or no subtitles available
					if (desriptors.isEmpty()) {
						MessageManager.showWarning(String.format("\"%s\" has not been found.", searchTask.query));
					}
					
					downloadPanel.close();
					return;
				}
				
				searchFieldCompletion.addTerm(descriptor.getTitle());
				Settings.getSettings().putStringList(Settings.SUBTITLE_HISTORY, searchFieldCompletion.getTerms());
				
				downloadPanel.setTitle(descriptor.getTitle());
				
				FetchSubtitleListTask fetchListTask = new FetchSubtitleListTask(descriptor, searchTask.client);
				fetchListTask.addPropertyChangeListener(new FetchSubtitleListTaskListener(downloadPanel));
				
				fetchListTask.execute();
			} catch (Exception e) {
				downloadPanel.close();
				
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
			
			// selected value or null if canceled by the user
			return selectDialog.getSelectedValue();
		}
		
	}
	

	private class FetchSubtitleListTaskListener extends SwingWorkerPropertyChangeAdapter {
		
		private final FileBotTab<SubtitleDownloadPanel> downloadPanel;
		
		
		public FetchSubtitleListTaskListener(FileBotTab<SubtitleDownloadPanel> downloadPanel) {
			this.downloadPanel = downloadPanel;
		}
		

		@Override
		public void done(PropertyChangeEvent evt) {
			// tab might have been closed
			if (downloadPanel.isClosed())
				return;
			
			FetchSubtitleListTask task = (FetchSubtitleListTask) evt.getSource();
			
			try {
				List<? extends SubtitleDescriptor> subtitleDescriptors = task.get();
				
				String info = (subtitleDescriptors.size() > 0) ? String.format("%d subtitles", subtitleDescriptors.size()) : "No subtitles found";
				
				historyPanel.add(task.getDescriptor().toString(), null, task.getClient().getIcon(), info, NumberFormat.getInstance().format(task.getDuration()) + " ms");
				
				if (subtitleDescriptors.isEmpty()) {
					MessageManager.showWarning(info);
					downloadPanel.close();
					return;
				}
				
				downloadPanel.setLoading(false);
				
				downloadPanel.getComponent().getPackagePanel().setTitle(info);
				downloadPanel.getComponent().addSubtitleDescriptors(subtitleDescriptors);
			} catch (Exception e) {
				downloadPanel.close();
				
				MessageManager.showWarning(FileBotUtil.getRootCause(e).getMessage());
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, e.toString(), e);
			}
		}
	}
	
}
