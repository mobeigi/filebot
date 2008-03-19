
package net.sourceforge.filebot.ui.panel.analyze.tools;


import java.awt.BorderLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import net.sourceforge.filebot.FileFormat;
import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.filebot.ui.FileBotTree;
import net.sourceforge.filebot.ui.transfer.DefaultTransferHandler;
import net.sourceforge.tuned.ui.LoadingOverlayPane;


public class TypePanel extends ToolPanel {
	
	private FileBotTree tree = new FileBotTree();
	
	
	public TypePanel() {
		super("Types");
		setLayout(new BorderLayout());
		
		JScrollPane sp = new JScrollPane(tree);
		sp.setBorder(BorderFactory.createEmptyBorder());
		add(new LoadingOverlayPane(sp, ResourceManager.getIcon("loading")), BorderLayout.CENTER);
		
		tree.setTransferHandler(new DefaultTransferHandler(null, new FileTreeExportHandler()));
		tree.setDragEnabled(true);
	}
	
	private UpdateTask latestUpdateTask;
	
	
	@Override
	public void update(Collection<File> files) {
		latestUpdateTask = new UpdateTask(files);
		
		tree.firePropertyChange(LoadingOverlayPane.LOADING_PROPERTY, false, true);
		latestUpdateTask.execute();
	}
	
	
	private class UpdateTask extends SwingWorker<DefaultTreeModel, Object> {
		
		private Collection<File> files;
		
		
		public UpdateTask(Collection<File> files) {
			this.files = files;
		}
		

		private boolean isLatest() {
			if (this == latestUpdateTask)
				return true;
			else
				return false;
		}
		

		@Override
		protected DefaultTreeModel doInBackground() throws Exception {
			Map<String, Collection<File>> map = new HashMap<String, Collection<File>>();
			
			for (File f : files) {
				String extension = FileFormat.getExtension(f);
				
				Collection<File> list = map.get(extension);
				
				if (list != null)
					list.add(f);
				else {
					list = new ArrayList<File>();
					list.add(f);
					map.put(extension, list);
				}
				
				if (!isLatest())
					return null;
			}
			
			DefaultMutableTreeNode root = new DefaultMutableTreeNode();
			Iterator<String> i = map.keySet().iterator();
			
			while (i.hasNext()) {
				String key = i.next();
				Collection<File> list = map.get(key);
				DefaultMutableTreeNode node = new DefaultMutableTreeNode();
				long size = 0;
				
				for (File f : list) {
					node.add(new DefaultMutableTreeNode(f));
					size += f.length();
				}
				
				node.setUserObject(key + " (" + FileFormat.formatNumberOfFiles(list.size()) + ", " + FileFormat.formatSize(size) + ")");
				root.add(node);
				
				if (!isLatest())
					return null;
			}
			
			return new DefaultTreeModel(root);
		}
		

		@Override
		protected void done() {
			try {
				DefaultTreeModel model = get();
				
				if (model == null)
					return;
				
				tree.setModel(model);
			} catch (Exception e) {
				// should not happen
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, e.toString(), e);
			}
			
			tree.firePropertyChange(LoadingOverlayPane.LOADING_PROPERTY, true, false);
		}
	}
	
}
