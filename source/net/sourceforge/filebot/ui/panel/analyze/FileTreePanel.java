
package net.sourceforge.filebot.ui.panel.analyze;


import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;

import net.sourceforge.filebot.FileBotUtil;
import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.filebot.ui.transfer.LoadAction;
import net.sourceforge.tuned.ui.LoadingOverlayPanel;


class FileTreePanel extends JPanel {
	
	private FileTree fileTree = new FileTree();
	
	private LoadingOverlayPanel loadingOverlay;
	
	
	public FileTreePanel() {
		super(new BorderLayout());
		fileTree.addPropertyChangeListener(FileTree.LOADING_PROPERTY, loadingOverlayUpdateListener);
		
		setBorder(BorderFactory.createTitledBorder("File Tree"));
		
		loadingOverlay = new LoadingOverlayPanel(new JScrollPane(fileTree), ResourceManager.getIcon("loading"));
		
		Box buttons = Box.createHorizontalBox();
		buttons.setBorder(new EmptyBorder(5, 5, 5, 5));
		buttons.add(Box.createGlue());
		buttons.add(new JButton(loadAction));
		buttons.add(Box.createHorizontalStrut(5));
		buttons.add(new JButton(clearAction));
		buttons.add(Box.createGlue());
		
		// Shortcut DELETE
		FileBotUtil.registerActionForKeystroke(fileTree, KeyStroke.getKeyStroke("pressed DELETE"), removeAction);
		
		add(loadingOverlay, BorderLayout.CENTER);
		add(buttons, BorderLayout.SOUTH);
	}
	
	private PropertyChangeListener loadingOverlayUpdateListener = new PropertyChangeListener() {
		
		public void propertyChange(PropertyChangeEvent evt) {
			Boolean loading = (Boolean) evt.getNewValue();
			
			loadingOverlay.setOverlayVisible(loading);
			loadingOverlay.updateOverlay();
		}
	};
	
	
	public FileTree getFileTree() {
		return fileTree;
	}
	
	private final LoadAction loadAction = new LoadAction(fileTree);
	
	private final AbstractAction clearAction = new AbstractAction("Clear", ResourceManager.getIcon("action.clear")) {
		
		public void actionPerformed(ActionEvent e) {
			fileTree.clear();
		}
	};
	
	private final AbstractAction removeAction = new AbstractAction("Remove") {
		
		public void actionPerformed(ActionEvent e) {
			if (fileTree.getSelectionCount() < 1)
				return;
			
			int row = fileTree.getMinSelectionRow();
			
			fileTree.removeTreeItems(fileTree.getSelectionPaths());
			
			int maxRow = fileTree.getRowCount() - 1;
			
			if (row > maxRow)
				row = maxRow;
			
			fileTree.setSelectionRow(row);
		}
	};
	
}
