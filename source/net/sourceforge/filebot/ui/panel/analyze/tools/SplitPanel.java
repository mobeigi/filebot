
package net.sourceforge.filebot.ui.panel.analyze.tools;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import net.sourceforge.filebot.FileFormat;
import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.filebot.ui.FileBotTree;
import net.sourceforge.filebot.ui.transfer.DefaultTransferHandler;
import net.sourceforge.tuned.ui.GradientStyle;
import net.sourceforge.tuned.ui.LoadingOverlayPanel;
import net.sourceforge.tuned.ui.notification.SeparatorBorder;


public class SplitPanel extends ToolPanel implements ChangeListener {
	
	private FileBotTree tree = new FileBotTree();
	
	private SpinnerNumberModel spinnerModel = new SpinnerNumberModel(4480, 0, Integer.MAX_VALUE, 100);
	
	
	public SplitPanel() {
		super("Split");
		setLayout(new BorderLayout());
		
		JScrollPane sp = new JScrollPane(tree);
		sp.setBorder(BorderFactory.createEmptyBorder());
		LoadingOverlayPanel loadingOverlay = new LoadingOverlayPanel(sp, ResourceManager.getIcon("loading"));
		JSpinner spinner = new JSpinner(spinnerModel);
		spinner.setMaximumSize(spinner.getPreferredSize());
		spinner.setEditor(new JSpinner.NumberEditor(spinner, "#"));
		
		Box spinnerBox = Box.createHorizontalBox();
		spinnerBox.add(Box.createGlue());
		spinnerBox.add(new JLabel("Split every"));
		spinnerBox.add(Box.createHorizontalStrut(5));
		spinnerBox.add(spinner);
		spinnerBox.add(Box.createHorizontalStrut(5));
		spinnerBox.add(new JLabel("MB."));
		spinnerBox.add(Box.createGlue());
		
		add(loadingOverlay, BorderLayout.CENTER);
		add(spinnerBox, BorderLayout.SOUTH);
		
		tree.setTransferHandler(new DefaultTransferHandler(null, new FileTreeExportHandler()));
		tree.setDragEnabled(true);
		
		Color beginColor = new Color(0, 0, 0, 90);
		SeparatorBorder separatorBorder = new SeparatorBorder(2, beginColor, GradientStyle.TOP_TO_BOTTOM, SeparatorBorder.Position.TOP);
		spinnerBox.setBorder(new CompoundBorder(separatorBorder, new EmptyBorder(6, 5, 7, 5)));
		
		setLoadingOverlayPane(loadingOverlay);
		spinnerModel.addChangeListener(this);
		spinner.setPreferredSize(new Dimension(80, 20));
	}
	

	/**
	 * callback when splitsize has been changed
	 */
	public void stateChanged(ChangeEvent e) {
		if (files != null)
			update();
	}
	

	private long getSplitSize() {
		return spinnerModel.getNumber().intValue() * FileFormat.MEGA;
	}
	
	private UpdateTask latestUpdateTask;
	
	private Collection<File> files;
	
	
	@Override
	public void update(Collection<File> files) {
		this.files = files;
		update();
	}
	

	private void update() {
		latestUpdateTask = new UpdateTask();
		firePropertyChange(LOADING_PROPERTY, null, true);
		latestUpdateTask.execute();
	}
	
	
	private class UpdateTask extends SwingWorker<DefaultTreeModel, Object> {
		
		private boolean isLatest() {
			if (this == latestUpdateTask)
				return true;
			else
				return false;
		}
		

		private void setLastChildUserObject(DefaultMutableTreeNode root, int part, long size) {
			DefaultMutableTreeNode node = ((DefaultMutableTreeNode) root.getLastChild());
			String uo = "Part " + part + " (" + FileFormat.formatSize(size) + ")";
			node.setUserObject(uo);
		}
		

		@Override
		protected DefaultTreeModel doInBackground() throws Exception {
			long currentSize = 0;
			
			DefaultMutableTreeNode root = new DefaultMutableTreeNode();
			DefaultMutableTreeNode first = new DefaultMutableTreeNode();
			root.add(first);
			DefaultMutableTreeNode remainder = new DefaultMutableTreeNode("Remainder");
			
			int p = 1;
			long splitSize = getSplitSize();
			
			for (File f : files) {
				long fileSize = f.length();
				DefaultMutableTreeNode fileNode = new DefaultMutableTreeNode(f);
				
				if (fileSize > splitSize)
					remainder.add(fileNode);
				else if (currentSize + fileSize <= splitSize) {
					currentSize += fileSize;
					((DefaultMutableTreeNode) root.getLastChild()).add(fileNode);
				} else {
					setLastChildUserObject(root, p, currentSize);
					
					currentSize = fileSize;
					p++;
					DefaultMutableTreeNode node = new DefaultMutableTreeNode();
					node.add(fileNode);
					root.add(node);
				}
				
				if (!isLatest())
					return null;
			}
			
			setLastChildUserObject(root, p, currentSize);
			
			if (!remainder.isLeaf())
				root.add(remainder);
			
			if (first.isLeaf())
				first.removeFromParent();
			
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
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, e.toString());
			}
			
			SplitPanel.this.firePropertyChange(LOADING_PROPERTY, null, false);
		}
	}
	
}
