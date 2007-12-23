
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

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import net.sourceforge.filebot.ui.panel.analyze.AnalyzePanel;
import net.sourceforge.filebot.ui.panel.create.CreatePanel;
import net.sourceforge.filebot.ui.panel.list.ListPanel;
import net.sourceforge.filebot.ui.panel.rename.RenamePanel;
import net.sourceforge.filebot.ui.panel.search.SearchPanel;
import net.sourceforge.filebot.ui.panel.sfv.SfvPanel;
import net.sourceforge.tuned.ui.FancyListCellRenderer;
import net.sourceforge.tuned.ui.GradientStyle;


public class FileBotPanelSelectionList extends JList {
	
	private static final int SELECTDELAY_ON_DRAG_OVER = 300;
	
	
	public FileBotPanelSelectionList() {
		DefaultListModel model = new DefaultListModel();
		setModel(model);
		
		setCellRenderer(new PanelCellRenderer());
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		setBorder(new EmptyBorder(4, 5, 4, 5));
		
		new DropTarget(this, new DragDropListener());
		
		model.addElement(new ListPanel());
		model.addElement(new RenamePanel());
		model.addElement(new AnalyzePanel());
		model.addElement(new SearchPanel());
		model.addElement(new CreatePanel());
		model.addElement(new SfvPanel());
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
			setText(panel.getText());
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
