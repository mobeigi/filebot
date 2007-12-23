
package net.sourceforge.filebot.ui;


import java.awt.Component;
import java.io.File;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.tuned.ui.FancyTreeCellRenderer;
import net.sourceforge.tuned.ui.GradientStyle;


public class FileBotTreeCellRenderer extends FancyTreeCellRenderer {
	
	public FileBotTreeCellRenderer() {
		super(GradientStyle.TOP_TO_BOTTOM);
		openIcon = ResourceManager.getIcon("tree.open");
		closedIcon = ResourceManager.getIcon("tree.closed");
		leafIcon = ResourceManager.getIcon("tree.leaf");
	}
	

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
		if (leaf && isFolder(value))
			super.getTreeCellRendererComponent(tree, value, sel, true, false, row, hasFocus);
		else
			super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
		
		return this;
	}
	

	private boolean isFolder(Object value) {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
		Object object = node.getUserObject();
		
		if (object instanceof File)
			return ((File) object).isDirectory();
		
		return false;
	}
	
}
