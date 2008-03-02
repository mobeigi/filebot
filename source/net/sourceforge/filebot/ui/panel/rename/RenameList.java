
package net.sourceforge.filebot.ui.panel.rename;


import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;

import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.filebot.ui.FileBotList;
import net.sourceforge.filebot.ui.transfer.LoadAction;


public abstract class RenameList extends FileBotList {
	
	public RenameList() {
		super(true, false, true);
		
		Box buttons = Box.createHorizontalBox();
		buttons.setBorder(new EmptyBorder(5, 5, 5, 5));
		buttons.add(Box.createGlue());
		buttons.add(new JButton(downAction));
		buttons.add(new JButton(upAction));
		buttons.add(Box.createHorizontalStrut(10));
		buttons.add(new JButton(loadAction));
		buttons.add(Box.createGlue());
		
		add(buttons, BorderLayout.SOUTH);
		
		JList list = getListComponent();
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		list.addMouseListener(dndReorderMouseAdapter);
		list.addMouseMotionListener(dndReorderMouseAdapter);
		
		JViewport viewport = (JViewport) list.getParent();
		viewport.setBackground(list.getBackground());
	}
	
	private final AbstractAction upAction = new AbstractAction(null, ResourceManager.getIcon("action.up")) {
		
		public void actionPerformed(ActionEvent e) {
			int index = getListComponent().getSelectedIndex();
			
			if (index <= 0) // first element
				return;
			
			Object object = getModel().remove(index);
			
			int newIndex = index - 1;
			getModel().add(newIndex, object);
			getListComponent().setSelectedIndex(newIndex);
		}
	};
	
	private final AbstractAction downAction = new AbstractAction(null, ResourceManager.getIcon("action.down")) {
		
		public void actionPerformed(ActionEvent e) {
			int index = getListComponent().getSelectedIndex();
			
			if (index >= getModel().getSize() - 1) // last element
				return;
			
			Object object = getModel().remove(index);
			
			int newIndex = index + 1;
			getModel().add(newIndex, object);
			getListComponent().setSelectedIndex(newIndex);
		}
	};
	
	protected final LoadAction loadAction = new LoadAction(this);
	
	private MouseAdapter dndReorderMouseAdapter = new MouseAdapter() {
		
		private int from = -1;
		
		
		@Override
		public void mousePressed(MouseEvent m) {
			from = getListComponent().getSelectedIndex();
		}
		

		@Override
		public void mouseDragged(MouseEvent m) {
			int to = getListComponent().getSelectedIndex();
			
			if (to == from)
				return;
			
			Object object = getModel().remove(from);
			getModel().add(to, object);
			from = to;
		}
	};
	
}
