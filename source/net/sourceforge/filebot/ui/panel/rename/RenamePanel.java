
package net.sourceforge.filebot.ui.panel.rename;


import java.awt.Insets;

import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.ui.FileBotPanel;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;


public class RenamePanel extends FileBotPanel {
	
	private RenameList<Object> namesList = new RenameList<Object>();
	private RenameList<FileEntry> filesList = new RenameList<FileEntry>();
	
	private MatchAction matchAction = new MatchAction(namesList.getModel(), filesList.getModel());
	
	private RenameAction renameAction = new RenameAction(namesList.getModel(), filesList.getModel());
	
	
	public RenamePanel() {
		super("Rename", ResourceManager.getIcon("panel.rename"));
		
		namesList.setTitle("Proposed");
		namesList.setTransferablePolicy(new NamesListTransferablePolicy(namesList));
		
		filesList.setTitle("Current");
		filesList.setTransferablePolicy(new FilesListTransferablePolicy(filesList.getModel()));
		
		JList namesListComponent = namesList.getListComponent();
		JList filesListComponent = filesList.getListComponent();
		
		RenameListCellRenderer cellrenderer = new RenameListCellRenderer(namesListComponent.getModel(), filesListComponent.getModel());
		
		namesListComponent.setCellRenderer(cellrenderer);
		filesListComponent.setCellRenderer(cellrenderer);
		
		ListSelectionModel selectionModel = new DefaultListSelectionModel();
		selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		namesListComponent.setSelectionModel(selectionModel);
		filesListComponent.setSelectionModel(selectionModel);
		
		// synchronize viewports
		new ViewPortSynchronizer((JViewport) namesListComponent.getParent(), (JViewport) filesListComponent.getParent());
		
		// create Match button
		JButton matchButton = new JButton(matchAction);
		matchButton.setVerticalTextPosition(SwingConstants.BOTTOM);
		matchButton.setHorizontalTextPosition(SwingConstants.CENTER);
		
		// create Rename button
		JButton renameButton = new JButton(renameAction);
		renameButton.setVerticalTextPosition(SwingConstants.BOTTOM);
		renameButton.setHorizontalTextPosition(SwingConstants.CENTER);
		
		setLayout(new MigLayout("fill, insets 0, gapx 10px", null, "align 33%"));
		
		add(namesList, "grow");
		
		// make buttons larger
		matchButton.setMargin(new Insets(3, 14, 2, 14));
		renameButton.setMargin(new Insets(6, 11, 2, 11));
		
		add(matchButton, "split 2, flowy, sizegroupx button");
		add(renameButton, "gapy 30px, sizegroupx button");
		
		add(filesList, "grow");
		
		namesList.getModel().addListEventListener(new RepaintHandler<Object>());
		filesList.getModel().addListEventListener(new RepaintHandler<FileEntry>());
	}
	
	
	private class RepaintHandler<E> implements ListEventListener<E> {
		
		@Override
		public void listChanged(ListEvent<E> listChanges) {
			namesList.repaint();
			filesList.repaint();
		}
		
	};
	
}
