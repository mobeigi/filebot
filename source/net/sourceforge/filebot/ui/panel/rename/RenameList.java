
package net.sourceforge.filebot.ui.panel.rename;


import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;

import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.ui.FileBotList;
import net.sourceforge.filebot.ui.panel.rename.entry.ListEntry;
import net.sourceforge.filebot.ui.transfer.LoadAction;
import net.sourceforge.filebot.ui.transfer.TransferablePolicy;


class RenameList<E extends ListEntry> extends FileBotList<E> {
	
	private JButton loadButton = new JButton();
	
	
	public RenameList() {
		Box buttons = Box.createHorizontalBox();
		buttons.setBorder(new EmptyBorder(5, 5, 5, 5));
		buttons.add(Box.createGlue());
		buttons.add(new JButton(downAction));
		buttons.add(new JButton(upAction));
		buttons.add(Box.createHorizontalStrut(10));
		buttons.add(loadButton);
		buttons.add(Box.createGlue());
		
		add(buttons, BorderLayout.SOUTH);
		
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		list.addMouseListener(dndReorderMouseAdapter);
		list.addMouseMotionListener(dndReorderMouseAdapter);
		
		JViewport viewport = (JViewport) list.getParent();
		viewport.setBackground(list.getBackground());
		
		getRemoveAction().setEnabled(true);
	}
	

	@Override
	public void setTransferablePolicy(TransferablePolicy transferablePolicy) {
		super.setTransferablePolicy(transferablePolicy);
		loadButton.setAction(new LoadAction(transferablePolicy));
	}
	

	public List<E> getEntries() {
		return new ArrayList<E>(getModel());
	}
	

	private boolean moveEntry(int fromIndex, int toIndex) {
		if (toIndex < 0 || toIndex >= getModel().size())
			return false;
		
		getModel().add(toIndex, getModel().remove(fromIndex));
		return true;
	}
	
	private final AbstractAction upAction = new AbstractAction(null, ResourceManager.getIcon("action.up")) {
		
		public void actionPerformed(ActionEvent e) {
			int selectedIndex = getListComponent().getSelectedIndex();
			int toIndex = selectedIndex + 1;
			
			if (moveEntry(selectedIndex, toIndex)) {
				getListComponent().setSelectedIndex(toIndex);
			}
		}
	};
	
	private final AbstractAction downAction = new AbstractAction(null, ResourceManager.getIcon("action.down")) {
		
		public void actionPerformed(ActionEvent e) {
			int selectedIndex = getListComponent().getSelectedIndex();
			int toIndex = selectedIndex - 1;
			
			if (moveEntry(selectedIndex, toIndex)) {
				getListComponent().setSelectedIndex(toIndex);
			}
		}
	};
	
	private final MouseAdapter dndReorderMouseAdapter = new MouseAdapter() {
		
		private int fromIndex = -1;
		
		
		@Override
		public void mousePressed(MouseEvent m) {
			fromIndex = getListComponent().getSelectedIndex();
		}
		

		@Override
		public void mouseDragged(MouseEvent m) {
			int toIndex = getListComponent().getSelectedIndex();
			
			if (toIndex == fromIndex)
				return;
			
			moveEntry(fromIndex, toIndex);
			
			fromIndex = toIndex;
		}
	};
	
}
