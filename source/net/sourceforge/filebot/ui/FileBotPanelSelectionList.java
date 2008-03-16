
package net.sourceforge.filebot.ui;


import java.awt.Color;
import java.awt.Component;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import net.sourceforge.filebot.ui.panel.analyze.AnalyzePanel;
import net.sourceforge.filebot.ui.panel.list.ListPanel;
import net.sourceforge.filebot.ui.panel.rename.RenamePanel;
import net.sourceforge.filebot.ui.panel.search.SearchPanel;
import net.sourceforge.filebot.ui.panel.sfv.SfvPanel;
import net.sourceforge.filebot.ui.panel.subtitle.SubtitlePanel;
import net.sourceforge.tuned.ui.FancyListCellRenderer;
import net.sourceforge.tuned.ui.GradientStyle;
import net.sourceforge.tuned.ui.SimpleListModel;


class FileBotPanelSelectionList extends JList {
	
	private static final int SELECTDELAY_ON_DRAG_OVER = 300;
	
	
	public FileBotPanelSelectionList() {
		setCellRenderer(new PanelCellRenderer());
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		setBorder(new EmptyBorder(4, 5, 4, 5));
		
		new DropTarget(this, new DragDropListener());
		
		List<FileBotPanel> panels = new ArrayList<FileBotPanel>();
		
		panels.add(new ListPanel());
		panels.add(new RenamePanel());
		panels.add(new AnalyzePanel());
		panels.add(new SearchPanel());
		panels.add(new SubtitlePanel());
		panels.add(new SfvPanel());
		
		setModel(new SimpleListModel(panels));
	}
	
	
	private class PanelCellRenderer extends FancyListCellRenderer {
		
		public PanelCellRenderer() {
			super(10, Color.decode("#163264"), GradientStyle.TOP_TO_BOTTOM);
			setVerticalTextPosition(SwingConstants.BOTTOM);
			setHorizontalTextPosition(SwingConstants.CENTER);
		}
		

		@Override
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			
			FileBotPanel panel = (FileBotPanel) value;
			setText(panel.getTitle());
			setIcon(panel.getIcon());
			
			return this;
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
