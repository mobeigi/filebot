
package net.sourceforge.filebot.ui.panel.rename;


import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;

import net.miginfocom.swing.MigLayout;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.ui.FileBotList;
import net.sourceforge.filebot.ui.transfer.LoadAction;
import net.sourceforge.filebot.ui.transfer.TransferablePolicy;
import ca.odell.glazedlists.EventList;


class RenameList<E> extends FileBotList<E> {
	
	public RenameList(EventList<E> model) {
		// replace default model with given model
		setModel(model);
		
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setFixedCellHeight(28);
		
		list.addMouseListener(dndReorderMouseAdapter);
		list.addMouseMotionListener(dndReorderMouseAdapter);
		
		getViewPort().setBackground(list.getBackground());
		
		getRemoveAction().setEnabled(true);
		
		JPanel buttonPanel = new JPanel(new MigLayout("insets 1.2mm, nogrid, fill", "align center"));
		
		buttonPanel.add(new JButton(downAction), "gap 10px");
		buttonPanel.add(new JButton(upAction), "gap 0");
		buttonPanel.add(new JButton(loadAction), "gap 10px");
		
		add(buttonPanel, BorderLayout.SOUTH);
	}
	

	@Override
	public void setTransferablePolicy(TransferablePolicy transferablePolicy) {
		super.setTransferablePolicy(transferablePolicy);
		loadAction.putValue(LoadAction.TRANSFERABLE_POLICY, transferablePolicy);
	}
	

	protected boolean moveEntry(int fromIndex, int toIndex) {
		if (toIndex < 0 || toIndex >= getModel().size())
			return false;
		
		// move element
		getModel().add(toIndex, getModel().remove(fromIndex));
		
		return true;
	}
	

	public JViewport getViewPort() {
		return listScrollPane.getViewport();
	}
	
	private final LoadAction loadAction = new LoadAction(null);
	
	private final AbstractAction upAction = new AbstractAction(null, ResourceManager.getIcon("action.up")) {
		
		public void actionPerformed(ActionEvent e) {
			int index = getListComponent().getSelectedIndex();
			
			if (moveEntry(index, index - 1)) {
				getListComponent().setSelectedIndex(index - 1);
			}
		}
	};
	
	private final AbstractAction downAction = new AbstractAction(null, ResourceManager.getIcon("action.down")) {
		
		public void actionPerformed(ActionEvent e) {
			int index = getListComponent().getSelectedIndex();
			
			if (moveEntry(index, index + 1)) {
				getListComponent().setSelectedIndex(index + 1);
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
