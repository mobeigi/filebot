
package net.sourceforge.filebot.ui.analyze;


import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import net.miginfocom.swing.MigLayout;
import net.sourceforge.filebot.ui.analyze.FileTree.FolderNode;
import net.sourceforge.filebot.ui.transfer.DefaultTransferHandler;
import net.sourceforge.tuned.FileUtilities;
import net.sourceforge.tuned.ui.GradientStyle;
import net.sourceforge.tuned.ui.LoadingOverlayPane;
import net.sourceforge.tuned.ui.notification.SeparatorBorder;


class SplitTool extends Tool<TreeModel> implements ChangeListener {
	
	private FileTree tree = new FileTree();
	
	private SpinnerNumberModel spinnerModel = new SpinnerNumberModel(4480, 0, Integer.MAX_VALUE, 100);
	
	
	public SplitTool() {
		super("Disks");
		
		JScrollPane treeScrollPane = new JScrollPane(tree);
		treeScrollPane.setBorder(new SeparatorBorder(2, new Color(0, 0, 0, 90), GradientStyle.TOP_TO_BOTTOM, SeparatorBorder.Position.BOTTOM));
		
		JSpinner spinner = new JSpinner(spinnerModel);
		spinner.setEditor(new JSpinner.NumberEditor(spinner, "#"));
		
		setLayout(new MigLayout("insets 0, nogrid, fill", "align center", "[fill][pref!]"));
		
		add(new LoadingOverlayPane(treeScrollPane, this), "grow, wrap");
		
		add(new JLabel("Split every"));
		add(spinner, "wmax 80, gap top rel, gap bottom unrel");
		add(new JLabel("MB."));
		
		tree.setTransferHandler(new DefaultTransferHandler(null, new FileTreeExportHandler()));
		tree.setDragEnabled(true);
		
		spinnerModel.addChangeListener(this);
	}
	
	
	private long getSplitSize() {
		return spinnerModel.getNumber().intValue() * FileUtilities.MEGA;
	}
	
	
	private FolderNode sourceModel = null;
	
	
	public void stateChanged(ChangeEvent evt) {
		if (sourceModel != null) {
			try {
				// update model in foreground, will be much faster than the initial load because length() is cached now
				setModel(createModelInBackground(sourceModel));
			} catch (InterruptedException e) {
				// will not happen
				Logger.getLogger(getClass().getName()).log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}
	
	
	@Override
	protected TreeModel createModelInBackground(FolderNode sourceModel) throws InterruptedException {
		this.sourceModel = sourceModel;
		
		FolderNode root = new FolderNode();
		int nextPart = 1;
		
		long splitSize = getSplitSize();
		
		List<File> currentPart = new ArrayList<File>(50);
		List<File> remainder = new ArrayList<File>(50);
		long totalSize = 0;
		
		for (Iterator<File> iterator = sourceModel.fileIterator(); iterator.hasNext();) {
			File file = iterator.next();
			
			long fileSize = file.length();
			
			if (fileSize > splitSize) {
				remainder.add(file);
				continue;
			}
			
			if (totalSize + fileSize > splitSize) {
				// part is full, add node and start with next one
				root.add(createStatisticsNode(String.format("Disk %d", nextPart++), currentPart));
				
				// reset total size and file list
				totalSize = 0;
				currentPart.clear();
			}
			
			totalSize += fileSize;
			currentPart.add(file);
			
			// unwind thread, if we have been cancelled
			if (Thread.interrupted()) {
				throw new InterruptedException();
			}
		}
		
		if (!currentPart.isEmpty()) {
			// add last part
			root.add(createStatisticsNode(String.format("Disk %d", nextPart++), currentPart));
		}
		
		if (!remainder.isEmpty()) {
			root.add(createStatisticsNode("Remainder", remainder));
		}
		
		return new DefaultTreeModel(root);
	}
	
	
	@Override
	protected void setModel(TreeModel model) {
		tree.setModel(model);
	}
	
}
