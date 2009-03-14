
package net.sourceforge.filebot.ui.panel.rename;


import static net.sourceforge.tuned.ui.LoadingOverlayPane.LOADING_PROPERTY;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.Settings;
import net.sourceforge.filebot.similarity.Match;
import net.sourceforge.filebot.similarity.NameSimilarityMetric;
import net.sourceforge.filebot.similarity.SimilarityMetric;
import net.sourceforge.filebot.ui.EpisodeExpressionFormat;
import net.sourceforge.filebot.ui.EpisodeFormatDialog;
import net.sourceforge.filebot.ui.SelectDialog;
import net.sourceforge.filebot.web.AnidbClient;
import net.sourceforge.filebot.web.Episode;
import net.sourceforge.filebot.web.EpisodeListClient;
import net.sourceforge.filebot.web.SearchResult;
import net.sourceforge.filebot.web.TVDotComClient;
import net.sourceforge.filebot.web.TVRageClient;
import net.sourceforge.filebot.web.TheTVDBClient;
import net.sourceforge.tuned.ExceptionUtilities;
import net.sourceforge.tuned.FileUtilities.NameWithoutExtensionFormat;
import net.sourceforge.tuned.PreferencesMap.PreferencesEntry;
import net.sourceforge.tuned.ui.ActionPopup;
import net.sourceforge.tuned.ui.LoadingOverlayPane;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;


public class RenamePanel extends JComponent {
	
	protected final RenameModel<Object, File> model = RenameModel.create();
	
	protected final NamesViewEventList namesView = new NamesViewEventList(this, model.names());
	
	protected final RenameList<String> namesList = new RenameList<String>(namesView);
	
	protected final RenameList<File> filesList = new RenameList<File>(model.files());
	
	protected final MatchAction matchAction = new MatchAction(model);
	
	protected final RenameAction renameAction = new RenameAction(RenameModel.wrap(namesView, model.files()));
	
	protected final PreferencesEntry<String> persistentFormat = Settings.userRoot().entry("rename.format");
	
	
	public RenamePanel() {
		
		namesList.setTitle("Proposed");
		namesList.setTransferablePolicy(new NamesListTransferablePolicy(model.names()));
		
		filesList.setTitle("Current");
		filesList.setTransferablePolicy(new FilesListTransferablePolicy(filesList.getModel()));
		
		namesView.setFormat(File.class, new NameWithoutExtensionFormat());
		
		// restore custom format
		restoreEpisodeFormat();
		
		RenameListCellRenderer cellrenderer = new RenameListCellRenderer(model);
		
		namesList.getListComponent().setCellRenderer(cellrenderer);
		filesList.getListComponent().setCellRenderer(cellrenderer);
		
		ListSelectionModel selectionModel = new DefaultListSelectionModel();
		selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		// use the same selection model for both lists to synchronize selection
		namesList.getListComponent().setSelectionModel(selectionModel);
		filesList.getListComponent().setSelectionModel(selectionModel);
		
		// synchronize viewports
		new ViewPortSynchronizer(namesList.getViewPort(), filesList.getViewPort());
		
		// create Match button
		JButton matchButton = new JButton(matchAction);
		matchButton.setVerticalTextPosition(SwingConstants.BOTTOM);
		matchButton.setHorizontalTextPosition(SwingConstants.CENTER);
		
		// create Rename button
		JButton renameButton = new JButton(renameAction);
		renameButton.setVerticalTextPosition(SwingConstants.BOTTOM);
		renameButton.setHorizontalTextPosition(SwingConstants.CENTER);
		
		// setup fetch action popup
		ActionPopup fetchPopup = createFetchPopup();
		
		namesList.getListComponent().setComponentPopupMenu(fetchPopup);
		matchButton.setComponentPopupMenu(fetchPopup);
		matchButton.addActionListener(showPopupAction);
		
		setLayout(new MigLayout("fill, insets dialog, gapx 10px", "[fill][align center, pref!][fill]", "align 33%"));
		
		add(new LoadingOverlayPane(namesList, namesList, "28px", "30px"), "grow, sizegroupx list");
		
		// make buttons larger
		matchButton.setMargin(new Insets(3, 14, 2, 14));
		renameButton.setMargin(new Insets(6, 11, 2, 11));
		
		add(matchButton, "split 2, flowy, sizegroupx button");
		add(renameButton, "gapy 30px, sizegroupx button");
		
		add(filesList, "grow, sizegroupx list");
		
		// repaint on change
		model.names().addListEventListener(new RepaintHandler<Object>());
		model.files().addListEventListener(new RepaintHandler<File>());
	}
	

	protected ActionPopup createFetchPopup() {
		final ActionPopup actionPopup = new ActionPopup("Fetch Episode List", ResourceManager.getIcon("action.fetch"));
		
		// create actions for match popup
		actionPopup.add(new AutoFetchEpisodeListAction(new TVRageClient()));
		actionPopup.add(new AutoFetchEpisodeListAction(new AnidbClient()));
		actionPopup.add(new AutoFetchEpisodeListAction(new TVDotComClient()));
		actionPopup.add(new AutoFetchEpisodeListAction(new TheTVDBClient(Settings.userRoot().get("thetvdb.apikey"))));
		
		actionPopup.addSeparator();
		actionPopup.addDescription(new JLabel("Options:"));
		
		actionPopup.add(new AbstractAction("Edit Format", ResourceManager.getIcon("action.format")) {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				Format format = EpisodeFormatDialog.showDialog(RenamePanel.this);
				
				if (format != null) {
					if (format instanceof EpisodeExpressionFormat) {
						persistentFormat.setValue(((EpisodeExpressionFormat) format).getFormat());
					} else {
						persistentFormat.remove();
					}
					
					namesView.setFormat(Episode.class, format);
				}
			}
		});
		
		return actionPopup;
	}
	

	private void restoreEpisodeFormat() {
		String format = persistentFormat.getValue();
		
		if (format != null) {
			try {
				namesView.setFormat(Episode.class, new EpisodeExpressionFormat(format));
			} catch (Exception e) {
				Logger.getLogger("ui").log(Level.WARNING, e.getMessage(), e);
			}
		}
	}
	
	protected final Action showPopupAction = new AbstractAction("Show Popup") {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			// show popup on actionPerformed only when names list is empty
			if (model.names().isEmpty() && !model.files().isEmpty()) {
				JComponent source = (JComponent) e.getSource();
				
				// display popup below component
				source.getComponentPopupMenu().show(source, -3, source.getHeight() + 4);
			}
		}
	};
	
	
	protected class AutoFetchEpisodeListAction extends AbstractAction {
		
		private final EpisodeListClient client;
		
		
		public AutoFetchEpisodeListAction(EpisodeListClient client) {
			super(client.getName(), client.getIcon());
			
			this.client = client;
			
			// disable action while episode list matcher is working
			namesList.addPropertyChangeListener(LOADING_PROPERTY, new PropertyChangeListener() {
				
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					// disable action while loading is in progress
					setEnabled(!(Boolean) evt.getNewValue());
				}
			});
		}
		

		@Override
		public void actionPerformed(ActionEvent evt) {
			// auto-match in progress
			namesList.firePropertyChange(LOADING_PROPERTY, false, true);
			
			// clear names list
			model.names().clear();
			
			AutoFetchEpisodeListMatcher worker = new AutoFetchEpisodeListMatcher(client, model.files(), matchAction.getMetrics()) {
				
				@Override
				protected void done() {
					try {
						List<Episode> episodes = new ArrayList<Episode>();
						List<File> files = new ArrayList<File>();
						
						for (Match<File, Episode> match : get()) {
							episodes.add(match.getCandidate());
							files.add(match.getValue());
						}
						
						model.clear();
						
						model.names().addAll(episodes);
						model.files().addAll(files);
						
						// add remaining file entries
						model.files().addAll(remainingFiles());
					} catch (Exception e) {
						Logger.getLogger("ui").log(Level.WARNING, ExceptionUtilities.getRootCauseMessage(e), e);
					} finally {
						// auto-match finished
						namesList.firePropertyChange(LOADING_PROPERTY, true, false);
					}
				}
				

				@Override
				protected SearchResult selectSearchResult(final String query, final Collection<SearchResult> searchResults) throws Exception {
					if (searchResults.size() == 1) {
						return searchResults.iterator().next();
					}
					
					final List<SearchResult> probableMatches = new LinkedList<SearchResult>();
					
					// use name similarity metric
					SimilarityMetric metric = new NameSimilarityMetric();
					
					// find probable matches using name similarity > 0.9
					for (SearchResult result : searchResults) {
						if (metric.getSimilarity(query, result.getName()) > 0.9) {
							probableMatches.add(result);
						}
					}
					
					if (probableMatches.size() == 1) {
						return probableMatches.get(0);
					}
					
					// show selection dialog on EDT
					final RunnableFuture<SearchResult> showSelectDialog = new FutureTask<SearchResult>(new Callable<SearchResult>() {
						
						@Override
						public SearchResult call() throws Exception {
							// multiple results have been found, user must select one
							SelectDialog<SearchResult> selectDialog = new SelectDialog<SearchResult>(SwingUtilities.getWindowAncestor(RenamePanel.this), probableMatches.isEmpty() ? searchResults : probableMatches);
							
							selectDialog.getHeaderLabel().setText(String.format("Shows matching '%s':", query));
							
							selectDialog.setVisible(true);
							
							// selected value or null if the dialog was canceled by the user
							return selectDialog.getSelectedValue();
						}
					});
					
					// run on EDT
					SwingUtilities.invokeAndWait(showSelectDialog);
					
					// selected value or null
					return showSelectDialog.get();
				}
			};
			
			worker.execute();
		}
	}
	

	protected class RepaintHandler<E> implements ListEventListener<E> {
		
		@Override
		public void listChanged(ListEvent<E> listChanges) {
			namesList.repaint();
			filesList.repaint();
		}
		
	};
	
}
