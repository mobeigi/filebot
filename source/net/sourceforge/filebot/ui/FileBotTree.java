
package net.sourceforge.filebot.ui;


import java.awt.Desktop;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.filebot.ui.transfer.TransferablePolicySupport;
import net.sourceforge.filebot.ui.transferablepolicies.NullTransferablePolicy;
import net.sourceforge.filebot.ui.transferablepolicies.TransferablePolicy;


public class FileBotTree extends JTree implements TransferablePolicySupport {
	
	private TransferablePolicy transferablePolicy = new NullTransferablePolicy();
	
	
	public FileBotTree() {
		super(new DefaultTreeModel(new DefaultMutableTreeNode()));
		getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
		setCellRenderer(new FileBotTreeCellRenderer());
		setShowsRootHandles(true);
		setRootVisible(false);
		setRowHeight(22);
		
		addMouseListener(new ExpandCollapsePopupListener());
	}
	

	public void clear() {
		DefaultTreeModel model = (DefaultTreeModel) getModel();
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
		
		root.removeAllChildren();
		model.reload(root);
	}
	

	public void setTransferablePolicy(TransferablePolicy transferablePolicy) {
		this.transferablePolicy = transferablePolicy;
	}
	

	public TransferablePolicy getTransferablePolicy() {
		return transferablePolicy;
	}
	

	public List<File> convertToList() {
		TreeNode node = (TreeNode) getModel().getRoot();
		
		return convertToList(node);
	}
	

	public List<File> convertToList(TreeNode node) {
		ArrayList<File> list = new ArrayList<File>();
		
		convertToListImpl((DefaultMutableTreeNode) node, list);
		
		return list;
	}
	

	private void convertToListImpl(DefaultMutableTreeNode node, List<File> list) {
		if (node.isLeaf()) {
			if (node.getUserObject() instanceof File) {
				File file = (File) node.getUserObject();
				
				if (file.isFile())
					list.add(file);
			}
		} else {
			for (int i = 0; i < node.getChildCount(); i++) {
				DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
				convertToListImpl(child, list);
			}
		}
	}
	

	@Override
	public String convertValueToText(Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
		if (value instanceof DefaultMutableTreeNode) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
			
			Object userObject = node.getUserObject();
			
			if ((userObject != null) && (userObject instanceof File)) {
				File file = (File) node.getUserObject();
				return file.getName();
			}
		}
		
		return value.toString();
	}
	

	public void expandOrCollapseAll(boolean expand) {
		TreeNode node = (TreeNode) getModel().getRoot();
		Enumeration<?> e = node.children();
		
		while (e.hasMoreElements()) {
			TreeNode n = (TreeNode) e.nextElement();
			TreePath path = new TreePath(node).pathByAddingChild(n);
			expandOrCollapseAllImpl(path, expand);
		}
	}
	

	private void expandOrCollapseAllImpl(TreePath parent, boolean expand) {
		// Traverse children
		TreeNode node = (TreeNode) parent.getLastPathComponent();
		if (node.getChildCount() >= 0)
			for (Enumeration<?> e = node.children(); e.hasMoreElements();) {
				TreeNode n = (TreeNode) e.nextElement();
				TreePath path = parent.pathByAddingChild(n);
				expandOrCollapseAllImpl(path, expand);
			}
		
		// Expansion or collapse must be done bottom-up
		if (expand)
			expandPath(parent);
		else
			collapsePath(parent);
	}
	
	
	private class ExpandCollapsePopup extends JPopupMenu {
		
		public ExpandCollapsePopup() {
			if (getSelectionCount() >= 1) {
				Object pathComponent = getLastSelectedPathComponent();
				
				if (pathComponent instanceof DefaultMutableTreeNode) {
					DefaultMutableTreeNode node = (DefaultMutableTreeNode) pathComponent;
					
					Object userObject = node.getUserObject();
					
					if (userObject instanceof File) {
						File file = (File) userObject;
						
						if (file.isFile()) {
							JMenuItem openItem = new JMenuItem(new OpenAction("Open", file));
							openItem.setFont(openItem.getFont().deriveFont(Font.BOLD));
							
							add(openItem);
							add(new OpenAction("Open Folder", file.getParentFile()));
							addSeparator();
						}
					}
				}
			}
			
			add(new ExpandOrCollapseAction(true));
			add(new ExpandOrCollapseAction(false));
		}
		
		
		private class OpenAction extends AbstractAction {
			
			private File file;
			
			
			public OpenAction(String text, File file) {
				super(text);
				this.file = file;
				
				if (file == null)
					setEnabled(false);
			}
			

			public void actionPerformed(ActionEvent event) {
				try {
					Desktop.getDesktop().open(file);
				} catch (Exception e) {
					MessageManager.showWarning(e.getMessage());
					Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING, e.getMessage());
				}
			}
		}
		

		private class ExpandOrCollapseAction extends AbstractAction {
			
			private boolean expand;
			
			
			public ExpandOrCollapseAction(boolean expand) {
				this.expand = expand;
				
				if (expand) {
					putValue(SMALL_ICON, ResourceManager.getIcon("tree.expand"));
					putValue(NAME, "Expand all");
				} else {
					putValue(SMALL_ICON, ResourceManager.getIcon("tree.collapse"));
					putValue(NAME, "Collapse all");
				}
			}
			

			public void actionPerformed(ActionEvent e) {
				expandOrCollapseAll(expand);
			}
		}
		
	}
	

	private class ExpandCollapsePopupListener extends MouseAdapter {
		
		@Override
		public void mouseReleased(MouseEvent e) {
			if (SwingUtilities.isRightMouseButton(e)) {
				ExpandCollapsePopup popup = new ExpandCollapsePopup();
				popup.show(e.getComponent(), e.getX(), e.getY());
			}
		}
		

		@Override
		public void mousePressed(MouseEvent e) {
			if (SwingUtilities.isRightMouseButton(e)) {
				TreePath path = getPathForLocation(e.getX(), e.getY());
				setSelectionPath(path);
			}
		}
	}
	
}
