
package net.sourceforge.filebot.ui.panel.analyze.tools;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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

import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.filebot.ui.FileBotTree;
import net.sourceforge.filebot.ui.transfer.DefaultTransferHandler;
import net.sourceforge.tuned.FileUtil;
import net.sourceforge.tuned.ui.GradientStyle;
import net.sourceforge.tuned.ui.LoadingOverlayPane;
import net.sourceforge.tuned.ui.notification.SeparatorBorder;


public class SplitPanel extends ToolPanel implements ChangeListener {
	
	private FileBotTree tree = new FileBotTree();
	
	private SpinnerNumberModel spinnerModel = new SpinnerNumberModel(4480, 0, Integer.MAX_VALUE, 100);
	
	
	public SplitPanel() {
		super("Split");
		setLayout(new BorderLayout());
		
		JScrollPane sp = new JScrollPane(tree);
		sp.setBorder(BorderFactory.createEmptyBorder());
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
		
		add(new LoadingOverlayPane(sp, ResourceManager.getIcon("loading")), BorderLayout.CENTER);
		add(spinnerBox, BorderLayout.SOUTH);
		
		tree.setTransferHandler(new DefaultTransferHandler(null, new FileTreeExportHandler()));
		tree.setDragEnabled(true);
		
		Color beginColor = new Color(0, 0, 0, 90);
		SeparatorBorder separatorBorder = new SeparatorBorder(2, beginColor, GradientStyle.TOP_TO_BOTTOM, SeparatorBorder.Position.TOP);
		spinnerBox.setBorder(new CompoundBorder(separatorBorder, new EmptyBorder(6, 5, 7, 5)));
		
		spinnerModel.addChangeListener(this);
		spinner.setPreferredSize(new Dimension(80, 20));
	}
	

	public void stateChanged(ChangeEvent e) {
		if (fileChache != null) {
			update();
		}
	}
	

	private long getSplitSize() {
		return spinnerModel.getNumber().intValue() * FileUtil.MEGA;
	}
	
	private UpdateTask updateTask;
	
	private Collection<File> fileChache;
	
	
	@Override
	public void update(Collection<File> files) {
		this.fileChache = files;
		update();
	}
	

	private synchronized void update() {
		if (updateTask != null) {
			updateTask.cancel(false);
		}
		
		updateTask = new UpdateTask(fileChache);
		
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
			
			List<List<File>> parts = new ArrayList<List<File>>();
			List<File> remainder = new ArrayList<File>();
			
			long splitSize = getSplitSize();
			
			long currentSize = 0;
			List<File> currentPart = null;
			
			for (File file : files) {
				long fileSize = file.length();
				
				if (fileSize > splitSize) {
					remainder.add(file);
					continue;
				}
				
				if (currentSize + fileSize > splitSize) {
					currentSize = 0;
					currentPart = null;
				}
				
				if (currentPart == null) {
					currentPart = new ArrayList<File>();
					parts.add(currentPart);
				}
				
				currentSize += fileSize;
				currentPart.add(file);
				
				if (isCancelled()) {
					return null;
				}
			}
			
			DefaultMutableTreeNode root = new DefaultMutableTreeNode();
			
			int count = 1;
			
			for (List<File> part : parts) {
				root.add(createTreeNode(String.format("Part %d", count), part));
				
				count++;
				
				if (isCancelled()) {
					return null;
				}
			}
			
			if (!remainder.isEmpty()) {
				root.add(createTreeNode("Remainder", remainder));
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
