
package net.sourceforge.filebot.ui.analyze;


import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import net.miginfocom.swing.MigLayout;
import net.sourceforge.filebot.ui.analyze.FileTree.FolderNode;
import net.sourceforge.filebot.ui.transfer.DefaultTransferHandler;
import net.sourceforge.tuned.FileUtilities;
import net.sourceforge.tuned.ui.LoadingOverlayPane;


class TypeTool extends Tool<TreeModel> {
	
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
	protected TreeModel createModelInBackground(FolderNode sourceModel) throws InterruptedException {
		Map<String, List<File>> map = new HashMap<String, List<File>>();
		
		for (Iterator<File> iterator = sourceModel.fileIterator(); iterator.hasNext();) {
			File file = iterator.next();
			
			String extension = FileUtilities.getExtension(file);
			if (extension != null) {
				extension = extension.toLowerCase();
			}
			
			List<File> files = map.get(extension);
			if (files == null) {
				files = new ArrayList<File>(50);
				map.put(extension, files);
			}
			
			files.add(file);
		}
		
		List<String> keys = new ArrayList<String>(map.keySet());
		
		// sort strings like always, handle null as empty string
		Collections.sort(keys, new Comparator<String>() {
			
			@Override
			public int compare(String s1, String s2) {
				return ((s1 != null) ? s1 : "").compareTo((s2 != null) ? s2 : "");
			}
		});
		
		// create tree model
		FolderNode root = new FolderNode();
		
		for (String key : keys) {
			root.add(createStatisticsNode(key, map.get(key)));
			
			// unwind thread, if we have been cancelled
			if (Thread.interrupted()) {
				throw new InterruptedException();
			}
		}
		
		return new DefaultTreeModel(root);
	}
	
	
	@Override
	protected void setModel(TreeModel model) {
		tree.setModel(model);
	}
	
}
