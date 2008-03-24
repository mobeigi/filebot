
package net.sourceforge.filebot.ui.panel.analyze.tools;


import java.io.File;
import java.util.Collection;

import javax.swing.JComponent;
import javax.swing.tree.DefaultMutableTreeNode;

import net.sourceforge.filebot.FileFormat;


public abstract class ToolPanel extends JComponent {
	
	private final String name;
	
	
	public ToolPanel(String name) {
		this.name = name;
	}
	

	public String getToolName() {
		return name;
	}
	

	public abstract void update(Collection<File> list);
	

	protected static DefaultMutableTreeNode createTreeNode(String name, Collection<File> files) {
		DefaultMutableTreeNode node = new DefaultMutableTreeNode();
		
		long totalSize = 0;
		
		for (File file : files) {
			node.add(new DefaultMutableTreeNode(file));
			totalSize += file.length();
		}
		
		String count = null;
		
		if (files.size() == 1) {
			count = String.format("%d file", files.size());
		} else {
			count = String.format("%d files", files.size());
		}
		
		node.setUserObject(String.format("%s (%s, %s)", name, count, FileFormat.formatSize(totalSize)));
		
		return node;
	}
}
