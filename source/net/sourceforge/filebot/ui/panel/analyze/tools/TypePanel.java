
package net.sourceforge.filebot.ui.panel.analyze.tools;


import java.awt.BorderLayout;
import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.filebot.ui.FileBotTree;
import net.sourceforge.filebot.ui.FileFormat;
import net.sourceforge.tuned.ui.LoadingOverlayPanel;


public class TypePanel extends ToolPanel {
	
	private FileBotTree tree = new FileBotTree();
	
	
	public TypePanel() {
		super("Types");
		setLayout(new BorderLayout());
		
		JScrollPane sp = new JScrollPane(tree);
		sp.setBorder(BorderFactory.createEmptyBorder());
		LoadingOverlayPanel loadingOverlay = new LoadingOverlayPanel(sp, ResourceManager.getIcon("loading"));
		add(loadingOverlay, BorderLayout.CENTER);
		
		setLoadingOverlayPane(loadingOverlay);
	}
	
	private UpdateTask latestUpdateTask;
	
	
	@Override
	public void update(Collection<File> files) {
		latestUpdateTask = new UpdateTask(files);
		
		firePropertyChange(LOADING_PROPERTY, null, true);
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
			TreeMap<String, Collection<File>> map = new TreeMap<String, Collection<File>>();
			
			for (File f : files) {
				String suffix = FileFormat.getSuffix(f);
				
				Collection<File> list = map.get(suffix);
				
				if (list != null)
					list.add(f);
				else {
					list = new LinkedList<File>();
					list.add(f);
					map.put(suffix, list);
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
				e.printStackTrace();
			}
			
			TypePanel.this.firePropertyChange(LOADING_PROPERTY, null, false);
		}
	}
	
}
