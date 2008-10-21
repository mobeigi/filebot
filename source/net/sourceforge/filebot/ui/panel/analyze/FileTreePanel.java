
package net.sourceforge.filebot.ui.panel.analyze;


import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

import net.miginfocom.swing.MigLayout;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.ui.transfer.LoadAction;
import net.sourceforge.tuned.ui.LoadingOverlayPane;
import net.sourceforge.tuned.ui.TunedUtil;


class FileTreePanel extends JPanel {
	
	private FileTree fileTree = new FileTree();
	
	
	public FileTreePanel() {
		super(new MigLayout("insets 0, nogrid, fill", "align center"));
		
		setBorder(BorderFactory.createTitledBorder("File Tree"));
		
		add(new LoadingOverlayPane(new JScrollPane(fileTree), ResourceManager.getIcon("loading")), "grow, wrap 1.2mm");
		add(new JButton(loadAction));
		add(new JButton(clearAction), "gap 1.2mm, wrap 1.2mm");
		
		// Shortcut DELETE
		TunedUtil.putActionForKeystroke(fileTree, KeyStroke.getKeyStroke("pressed DELETE"), removeAction);
	}
	

	public FileTree getFileTree() {
		return fileTree;
	}
	
	private final LoadAction loadAction = new LoadAction(fileTree.getTransferablePolicy());
	
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
