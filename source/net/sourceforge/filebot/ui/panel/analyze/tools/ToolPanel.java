
package net.sourceforge.filebot.ui.panel.analyze.tools;


import java.io.File;
import java.util.Collection;

import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;

import net.sourceforge.tuned.FileUtil;


public abstract class ToolPanel extends JPanel {
	
	private final String name;
	
	
	public ToolPanel(String name) {
		super(null);
		this.name = name;
	}
	

	public String getToolName() {
		return name;
	}
	

	public abstract void update(Collection<File> list);
	

	protected DefaultMutableTreeNode createTreeNode(String name, Collection<File> files) {
		DefaultMutableTreeNode node = new DefaultMutableTreeNode();
		
		long totalSize = 0;
		
		for (File file : files) {
			node.add(new DefaultMutableTreeNode(file));
			totalSize += file.length();
		}
		
		// format the number of files string (e.g. 1 file, 2 files, ...)
		String numberOfFiles = String.format("%,d %s", files.size(), files.size() == 1 ? "file" : "files");
		
		// set node text (e.g. txt (1 file, 42 Byte))
		node.setUserObject(String.format("%s (%s, %s)", name, numberOfFiles, FileUtil.formatSize(totalSize)));
		
		return node;
	}
}
