
package net.sourceforge.filebot.ui.panel.analyze.tools;


import java.awt.BorderLayout;
import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.ui.FileBotTree;
import net.sourceforge.filebot.ui.transfer.DefaultTransferHandler;
import net.sourceforge.tuned.FileUtil;
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
	
	private UpdateTask updateTask = null;
	
	
	@Override
	public synchronized void update(Collection<File> files) {
		if (updateTask != null) {
			updateTask.cancel(false);
		}
		
		updateTask = new UpdateTask(files);
		
		tree.firePropertyChange(LoadingOverlayPane.LOADING_PROPERTY, false, true);
		updateTask.execute();
	}
	
	
	private class UpdateTask extends SwingWorker<DefaultTreeModel, Void> {
		
		private final Collection<File> files;
		
		
		public UpdateTask(Collection<File> files) {
			this.files = files;
		}
		

		@Override
		protected DefaultTreeModel doInBackground() throws Exception {
			SortedMap<String, SortedSet<File>> map = new TreeMap<String, SortedSet<File>>();
			
			for (File file : files) {
				String extension = FileUtil.getExtension(file);
				
				SortedSet<File> set = map.get(extension);
				
				if (set == null) {
					set = new TreeSet<File>();
					map.put(extension, set);
				}
				
				set.add(file);
				
				if (isCancelled()) {
					return null;
				}
			}
			
			DefaultMutableTreeNode root = new DefaultMutableTreeNode();
			
			for (Map.Entry<String, SortedSet<File>> entry : map.entrySet()) {
				
				root.add(createTreeNode(entry.getKey(), entry.getValue()));
				
				if (isCancelled()) {
					return null;
				}
			}
			
			return new DefaultTreeModel(root);
		}
		

		@Override
		protected void done() {
			if (isCancelled()) {
				return;
			}
			
			try {
				tree.setModel(get());
			} catch (Exception e) {
				// should not happen
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, e.toString(), e);
			}
			
			tree.firePropertyChange(LoadingOverlayPane.LOADING_PROPERTY, true, false);
		}
	}
	
}
