
package net.sourceforge.filebot.ui.panel.analyze;


import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import net.miginfocom.swing.MigLayout;
import net.sourceforge.filebot.ui.panel.analyze.FileTree.FolderNode;
import net.sourceforge.filebot.ui.transfer.DefaultTransferHandler;
import net.sourceforge.tuned.FileUtil;
import net.sourceforge.tuned.ui.LoadingOverlayPane;


public class TypeTool extends Tool<TreeModel> {
	
	private FileTree tree = new FileTree();
	
	
	public TypeTool() {
		super("Types");
		
		setLayout(new MigLayout("insets 0, fill"));
		
		JScrollPane treeScrollPane = new JScrollPane(tree);
		treeScrollPane.setBorder(BorderFactory.createEmptyBorder());
		add(new LoadingOverlayPane(treeScrollPane, this), "grow");
		
		tree.setTransferHandler(new DefaultTransferHandler(null, new FileTreeExportHandler()));
		tree.setDragEnabled(true);
	}
	

	@Override
	protected TreeModel createModelInBackground(FolderNode sourceModel, Cancellable cancellable) {
		TreeMap<String, List<File>> map = new TreeMap<String, List<File>>();
		
		for (Iterator<File> iterator = sourceModel.fileIterator(); iterator.hasNext() && !cancellable.isCancelled();) {
			File file = iterator.next();
			String extension = FileUtil.getExtension(file);
			
			List<File> files = map.get(extension);
			
			if (files == null) {
				files = new ArrayList<File>(50);
				map.put(extension, files);
			}
			
			files.add(file);
		}
		
		FolderNode root = new FolderNode();
		
		for (Entry<String, List<File>> entry : map.entrySet()) {
			root.add(createStatisticsNode(entry.getKey(), entry.getValue()));
		}
		
		return new DefaultTreeModel(root);
	}
	

	@Override
	protected void setModel(TreeModel model) {
		tree.setModel(model);
	}
	
}
