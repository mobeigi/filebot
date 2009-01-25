
package net.sourceforge.filebot.ui.panel.rename;


import static javax.swing.SwingUtilities.getWindowAncestor;
import static net.sourceforge.tuned.ui.LoadingOverlayPane.LOADING_PROPERTY;
import static net.sourceforge.filebot.FileBotUtilities.*;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.Settings;
import net.sourceforge.filebot.similarity.Match;
import net.sourceforge.filebot.ui.FileBotPanel;
import net.sourceforge.filebot.web.AnidbClient;
import net.sourceforge.filebot.web.Episode;
import net.sourceforge.filebot.web.EpisodeListClient;
import net.sourceforge.filebot.web.TVRageClient;
import net.sourceforge.filebot.web.TheTVDBClient;
import net.sourceforge.tuned.ExceptionUtil;
import net.sourceforge.tuned.ui.ActionPopup;
import net.sourceforge.tuned.ui.LoadingOverlayPane;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;


public class RenamePanel extends FileBotPanel {
	
	private RenameModel model = new RenameModel();
	
	private RenameList<Object> namesList = new RenameList<Object>(model.names());
	
	private RenameList<FileEntry> filesList = new RenameList<FileEntry>(model.files());
	
	private MatchAction matchAction = new MatchAction(model);
	
	private RenameAction renameAction = new RenameAction(model);
	
	private ActionPopup matchActionPopup = new ActionPopup("Fetch Episode List", ResourceManager.getIcon("action.match.small"));
	
	
	public RenamePanel() {
		super("Rename", ResourceManager.getIcon("panel.rename"));
		
		namesList.setTitle("Proposed");
		namesList.setTransferablePolicy(new NamesListTransferablePolicy(namesList));
		
		filesList.setTitle("Current");
		filesList.setTransferablePolicy(new FilesListTransferablePolicy(filesList.getModel()));
		
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
		
		// create actions for match popup
		matchActionPopup.add(new AutoFetchEpisodeListAction(new TVRageClient()));
		matchActionPopup.add(new AutoFetchEpisodeListAction(new AnidbClient()));
		matchActionPopup.add(new AutoFetchEpisodeListAction(new TheTVDBClient(Settings.userRoot().get("thetvdb.apikey"))));
		
		// set match action popup
		matchButton.setComponentPopupMenu(matchActionPopup);
		matchButton.addActionListener(showPopupAction);
		
		setLayout(new MigLayout("fill, insets dialog, gapx 10px", null, "align 33%"));
		
		add(new LoadingOverlayPane(namesList, this, "28px", "30px"), "grow, sizegroupx list");
		
		// make buttons larger
		matchButton.setMargin(new Insets(3, 14, 2, 14));
		renameButton.setMargin(new Insets(6, 11, 2, 11));
		
		add(matchButton, "split 2, flowy, sizegroupx button");
		add(renameButton, "gapy 30px, sizegroupx button");
		
		add(filesList, "grow, sizegroupx list");
		
		// repaint on change
		model.names().addListEventListener(new RepaintHandler<Object>());
		model.files().addListEventListener(new RepaintHandler<FileEntry>());
	}
	
	protected final Action showPopupAction = new AbstractAction("Show Popup") {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			// show popup on actionPerformed only when names list is empty
			if (model.names().isEmpty()) {
				JComponent source = (JComponent) e.getSource();
				
				// display popup below component
				source.getComponentPopupMenu().show(source, -3, source.getHeight() + 4);
			}
		}
	};
	
	private boolean autoMatchInProgress = false;
	
	
	protected void setAutoMatchInProgress(boolean flag) {
		this.autoMatchInProgress = flag;
		firePropertyChange(LOADING_PROPERTY, !flag, flag);
		matchActionPopup.setStatus(flag ? "in progress" : null);
	}
	

	protected boolean isAutoMatchInProgress() {
		return autoMatchInProgress;
	}
	
	
	protected class AutoFetchEpisodeListAction extends AbstractAction {
		
		private final EpisodeListClient client;
		
		
		public AutoFetchEpisodeListAction(EpisodeListClient client) {
			super(client.getName(), client.getIcon());
			
			this.client = client;
		}
		

		@Override
		public void actionPerformed(ActionEvent evt) {
			if (model.files().isEmpty() || isAutoMatchInProgress())
				return;
			
			AutoEpisodeListMatcher worker = new AutoEpisodeListMatcher(client, new ArrayList<FileEntry>(model.files()), matchAction.getMetrics()) {
				
				@Override
				protected void done() {
					// background worker is finished
					setAutoMatchInProgress(false);
					
					try {
						List<StringEntry> names = new ArrayList<StringEntry>();
						List<FileEntry> files = new ArrayList<FileEntry>();
						
						List<StringEntry> invalidNames = new ArrayList<StringEntry>();
						
						for (Match<FileEntry, Episode> match : get()) {
							StringEntry name = new StringEntry(match.getCandidate());
							
							if (isInvalidFileName(name.toString())) {
								invalidNames.add(name);
							}
							
							names.add(new StringEntry(name));
							files.add(match.getValue());
						}
						
						if (!invalidNames.isEmpty()) {
							ValidateNamesDialog dialog = new ValidateNamesDialog(getWindowAncestor(RenamePanel.this), invalidNames);
							dialog.setVisible(true);
							
							if (dialog.isCancelled()) {
								// don't touch model
								return;
							}
						}
						
						model.clear();
						
						model.names().addAll(names);
						model.files().addAll(files);
						
						// add remaining file entries again
						model.files().addAll(remainingFiles());
					} catch (Exception e) {
						Logger.getLogger("ui").log(Level.WARNING, ExceptionUtil.getRootCause(e).getMessage(), e);
					}
				}
			};
			
			worker.execute();
			
			// background worker started
			setAutoMatchInProgress(true);
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
