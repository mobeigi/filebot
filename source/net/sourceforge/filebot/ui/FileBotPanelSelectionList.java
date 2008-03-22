
package net.sourceforge.filebot.ui;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import net.sourceforge.tuned.ui.DefaultFancyListCellRenderer;
import net.sourceforge.tuned.ui.SimpleListModel;


class FileBotPanelSelectionList extends JList {
	
	private static final int SELECTDELAY_ON_DRAG_OVER = 300;
	
	
	public FileBotPanelSelectionList() {
		setCellRenderer(new PanelCellRenderer());
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		setBorder(new EmptyBorder(4, 5, 4, 5));
		
		new DropTarget(this, new DragDropListener());
		
		SimpleListModel model = new SimpleListModel();
		
		for (FileBotPanel panel : FileBotPanel.getAvailablePanels()) {
			model.add(panel);
		}
		
		setModel(model);
	}
	
	
	private class PanelCellRenderer extends DefaultFancyListCellRenderer {
		
		public PanelCellRenderer() {
			super(BorderLayout.CENTER, 10, 0, Color.decode("#163264"));
			
			setHighlightingEnabled(false);
			
			setVerticalTextPosition(SwingConstants.BOTTOM);
			setHorizontalTextPosition(SwingConstants.CENTER);
		}
		

		@Override
		public void configureListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			super.configureListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			
			FileBotPanel panel = (FileBotPanel) value;
			setText(panel.getPanelName());
			setIcon(panel.getIcon());
		}
		
	}
	

	private class DragDropListener extends DropTargetAdapter implements ActionListener {
		
		private boolean selectEnabled = false;
		
		private Timer timer = new Timer(SELECTDELAY_ON_DRAG_OVER, this);
		
		
		public DragDropListener() {
			timer.setRepeats(false);
		}
		

		public void actionPerformed(ActionEvent e) {
			selectEnabled = true;
			
			// bring window to front when on dnd
			SwingUtilities.getWindowAncestor(FileBotPanelSelectionList.this).toFront();
		}
		

		@Override
		public void dragOver(DropTargetDragEvent dtde) {
			if (selectEnabled) {
				int index = locationToIndex(dtde.getLocation());
				setSelectedIndex(index);
			}
		}
		

		@Override
		public void dragEnter(DropTargetDragEvent dtde) {
			timer.start();
		}
		

		@Override
		public void dragExit(DropTargetEvent dte) {
			timer.stop();
			selectEnabled = false;
		}
		

		public void drop(DropTargetDropEvent dtde) {
			
		}
		
	}
	
}
