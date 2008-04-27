
package net.sourceforge.filebot.ui.panel.rename;


import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.filebot.ui.FileBotPanel;


public class RenamePanel extends FileBotPanel {
	
	private RenameList namesList = new RenameList();
	private RenameList filesList = new RenameList();
	
	private MatchAction matchAction = new MatchAction(namesList, filesList);
	
	private RenameAction renameAction = new RenameAction(namesList, filesList);
	
	private SimilarityPanel similarityPanel;
	
	private ViewPortSynchronizer viewPortSynchroniser;
	
	
	public RenamePanel() {
		super("Rename", ResourceManager.getIcon("panel.rename"));
		
		namesList.setTitle("Names");
		namesList.setTransferablePolicy(new NamesListTransferablePolicy(namesList));
		
		filesList.setTitle("Files");
		filesList.setTransferablePolicy(new FilesListTransferablePolicy(filesList.getModel()));
		
		RenameListCellRenderer cellrenderer = new RenameListCellRenderer(namesList.getModel(), filesList.getModel());
		
		namesList.getListComponent().setCellRenderer(cellrenderer);
		filesList.getListComponent().setCellRenderer(cellrenderer);
		
		JList list1 = namesList.getListComponent();
		JList list2 = filesList.getListComponent();
		
		ListSelectionModel selectionModel = new DefaultListSelectionModel();
		selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		namesList.getListComponent().setSelectionModel(selectionModel);
		filesList.getListComponent().setSelectionModel(selectionModel);
		
		viewPortSynchroniser = new ViewPortSynchronizer((JViewport) list1.getParent(), (JViewport) list2.getParent());
		
		similarityPanel = new SimilarityPanel(list1, list2);
		
		similarityPanel.setVisible(false);
		similarityPanel.setMetrics(matchAction.getMetrics());
		
		Box box = new Box(BoxLayout.X_AXIS);
		
		box.add(namesList);
		box.add(Box.createHorizontalStrut(10));
		box.add(createCenterBox());
		box.add(Box.createHorizontalStrut(10));
		
		Box subBox = Box.createVerticalBox();
		
		subBox.add(similarityPanel);
		subBox.add(filesList);
		
		box.add(subBox);
		
		add(box, BorderLayout.CENTER);
		
		namesList.getModel().addListDataListener(repaintOnDataChange);
		filesList.getModel().addListDataListener(repaintOnDataChange);
	}
	

	private Box createCenterBox() {
		Box centerBox = Box.createVerticalBox();
		
		JButton matchButton = new JButton(matchAction);
		matchButton.addMouseListener(new MatchPopupListener());
		matchButton.setMargin(new Insets(3, 14, 2, 14));
		matchButton.setVerticalTextPosition(SwingConstants.BOTTOM);
		matchButton.setHorizontalTextPosition(SwingConstants.CENTER);
		matchButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		JButton renameButton = new JButton(renameAction);
		renameButton.setMargin(new Insets(6, 11, 2, 11));
		renameButton.setVerticalTextPosition(SwingConstants.BOTTOM);
		renameButton.setHorizontalTextPosition(SwingConstants.CENTER);
		renameButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		centerBox.add(Box.createGlue());
		centerBox.add(matchButton);
		centerBox.add(Box.createVerticalStrut(30));
		centerBox.add(renameButton);
		centerBox.add(Box.createGlue());
		centerBox.add(Box.createGlue());
		
		return centerBox;
	}
	
	private ListDataListener repaintOnDataChange = new ListDataListener() {
		
		public void contentsChanged(ListDataEvent e) {
			
		}
		

		public void intervalAdded(ListDataEvent e) {
			repaintBoth();
		}
		

		public void intervalRemoved(ListDataEvent e) {
			repaintBoth();
		}
		

		public void repaintBoth() {
			namesList.repaint();
			filesList.repaint();
		}
		
	};
	
	
	private class MatcherSelectPopup extends JPopupMenu {
		
		public MatcherSelectPopup() {
			JMenuItem names2files = new JMenuItem(new SetNames2FilesAction(true));
			JMenuItem files2names = new JMenuItem(new SetNames2FilesAction(false));
			
			if (matchAction.isMatchName2File())
				highlight(names2files);
			else
				highlight(files2names);
			
			add(names2files);
			add(files2names);
			
			addSeparator();
			add(new ToggleSimilarityAction(!similarityPanel.isVisible()));
		}
		

		public void highlight(JMenuItem item) {
			item.setFont(item.getFont().deriveFont(Font.BOLD));
		}
		
		
		private class SetNames2FilesAction extends AbstractAction {
			
			private boolean names2files;
			
			
			public SetNames2FilesAction(boolean names2files) {
				this.names2files = names2files;
				
				if (names2files) {
					putValue(SMALL_ICON, ResourceManager.getIcon("action.match.name2file"));
					putValue(NAME, MatchAction.MATCH_NAMES_2_FILES_DESCRIPTION);
				} else {
					putValue(SMALL_ICON, ResourceManager.getIcon("action.match.file2name"));
					putValue(NAME, MatchAction.MATCH_FILES_2_NAMES_DESCRIPTION);
				}
			}
			

			public void actionPerformed(ActionEvent e) {
				matchAction.setMatchName2File(names2files);
			}
		}
		

		private class ToggleSimilarityAction extends AbstractAction {
			
			private boolean showSimilarityPanel;
			
			
			public ToggleSimilarityAction(boolean showSimilarityPanel) {
				this.showSimilarityPanel = showSimilarityPanel;
				
				if (showSimilarityPanel) {
					putValue(NAME, "Show Similarity");
				} else {
					putValue(NAME, "Hide Similarity");
				}
			}
			

			public void actionPerformed(ActionEvent e) {
				if (showSimilarityPanel) {
					viewPortSynchroniser.setEnabled(false);
					similarityPanel.hook();
					similarityPanel.setVisible(true);
				} else {
					similarityPanel.setVisible(false);
					similarityPanel.unhook();
					viewPortSynchroniser.setEnabled(true);
				}
			}
		}
	}
	

	private class MatchPopupListener extends MouseAdapter {
		
		@Override
		public void mouseReleased(MouseEvent e) {
			if (SwingUtilities.isRightMouseButton(e)) {
				MatcherSelectPopup popup = new MatcherSelectPopup();
				popup.show(e.getComponent(), e.getX(), e.getY());
			}
		}
	}
	
}
