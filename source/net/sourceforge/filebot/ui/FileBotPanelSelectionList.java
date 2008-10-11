
package net.sourceforge.filebot.ui;


import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;

import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import net.sourceforge.tuned.ui.DefaultFancyListCellRenderer;
import net.sourceforge.tuned.ui.TunedUtil;
import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.EventListModel;


class FileBotPanelSelectionList extends JList {
	
	private static final int SELECTDELAY_ON_DRAG_OVER = 300;
	
	private final EventList<FileBotPanel> panelModel = new BasicEventList<FileBotPanel>();
	
	
	public FileBotPanelSelectionList() {
		
		setModel(new EventListModel<FileBotPanel>(panelModel));
		
		setCellRenderer(new PanelCellRenderer());
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		setBorder(new EmptyBorder(4, 5, 4, 5));
		
		// initialize "drag over" panel selection
		new DropTarget(this, new DragDropListener());
	}
	

	public EventList<FileBotPanel> getPanelModel() {
		return panelModel;
	}
	
	
	private static class PanelCellRenderer extends DefaultFancyListCellRenderer {
		
		public PanelCellRenderer() {
			super(10, 0, new Color(0x163264));
			
			// center labels in list
			setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
			
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
	

	private class DragDropListener extends DropTargetAdapter {
		
		private boolean selectEnabled = false;
		
		private Timer dragEnterTimer;
		
		
		@Override
		public void dragOver(DropTargetDragEvent dtde) {
			if (selectEnabled) {
				int index = locationToIndex(dtde.getLocation());
				setSelectedIndex(index);
			}
		}
		

		@Override
		public void dragEnter(DropTargetDragEvent dtde) {
			dragEnterTimer = TunedUtil.invokeLater(SELECTDELAY_ON_DRAG_OVER, new Runnable() {
				
				@Override
				public void run() {
					selectEnabled = true;
					
					// bring window to front when on dnd
					SwingUtilities.getWindowAncestor(FileBotPanelSelectionList.this).toFront();
				}
			});
		}
		

		@Override
		public void dragExit(DropTargetEvent dte) {
			selectEnabled = false;
			
			if (dragEnterTimer != null) {
				dragEnterTimer.stop();
			}
		}
		

		@Override
		public void drop(DropTargetDropEvent dtde) {
			
		}
		
	}
	
}
