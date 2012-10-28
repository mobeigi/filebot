
package net.sourceforge.filebot.ui.analyze;


import static net.sourceforge.filebot.ui.transfer.BackgroundFileTransferablePolicy.*;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

import net.miginfocom.swing.MigLayout;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.ui.transfer.DefaultTransferHandler;
import net.sourceforge.filebot.ui.transfer.LoadAction;
import net.sourceforge.tuned.ui.LoadingOverlayPane;
import net.sourceforge.tuned.ui.TunedUtilities;


class FileTreePanel extends JComponent {
	
	private FileTree fileTree = new FileTree();
	
	private FileTreeTransferablePolicy transferablePolicy = new FileTreeTransferablePolicy(fileTree);
	
	
	public FileTreePanel() {
		fileTree.setTransferHandler(new DefaultTransferHandler(transferablePolicy, null));
		
		setBorder(BorderFactory.createTitledBorder("File Tree"));
		
		setLayout(new MigLayout("insets 0, nogrid, fill", "align center", "[fill][pref!]"));
		add(new LoadingOverlayPane(new JScrollPane(fileTree), this), "grow, wrap 1.2mm");
		add(new JButton(loadAction));
		add(new JButton(clearAction), "gap 1.2mm, wrap 1.2mm");
		
		// forward loading events
		transferablePolicy.addPropertyChangeListener(new PropertyChangeListener() {
			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if (LOADING_PROPERTY.equals(evt.getPropertyName())) {
					firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
				}
			}
		});
		
		// update tree when loading is finished
		transferablePolicy.addPropertyChangeListener(new PropertyChangeListener() {
			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if (LOADING_PROPERTY.equals(evt.getPropertyName()) && !(Boolean) evt.getNewValue()) {
					fireFileTreeChange();
				}
			}
		});
		
		// Shortcut DELETE
		TunedUtilities.installAction(fileTree, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), removeAction);
		TunedUtilities.installAction(fileTree, KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), removeAction);
	}
	
	
	public FileTree getFileTree() {
		return fileTree;
	}
	
	
	public FileTreeTransferablePolicy getTransferablePolicy() {
		return transferablePolicy;
	}
	
	private final LoadAction loadAction = new LoadAction(transferablePolicy);
	
	private final AbstractAction clearAction = new AbstractAction("Clear", ResourceManager.getIcon("action.clear")) {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			transferablePolicy.reset();
			fileTree.clear();
			fireFileTreeChange();
		}
	};
	
	private final AbstractAction removeAction = new AbstractAction("Remove") {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			if (fileTree.getSelectionCount() < 1)
				return;
			
			int row = fileTree.getMinSelectionRow();
			
			fileTree.removeTreeNode(fileTree.getSelectionPaths());
			
			int maxRow = fileTree.getRowCount() - 1;
			
			if (row > maxRow)
				row = maxRow;
			
			fileTree.setSelectionRow(row);
			
			fireFileTreeChange();
		}
	};
	
	
	private void fireFileTreeChange() {
		firePropertyChange("filetree", null, fileTree);
	}
	
}
