
package net.sourceforge.filebot.ui.rename;


import static java.util.Collections.*;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;

import net.miginfocom.swing.MigLayout;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.ui.FileBotList;
import net.sourceforge.filebot.ui.transfer.LoadAction;
import net.sourceforge.filebot.ui.transfer.TransferablePolicy;
import ca.odell.glazedlists.EventList;


class RenameList<E> extends FileBotList<E> {
	
	private JPanel buttonPanel;
	
	
	public RenameList(EventList<E> model) {
		// replace default model with given model
		setModel(model);
		
		list.setFixedCellHeight(28); // need a fixed cell high for high performance scrolling
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		list.addMouseListener(dndReorderMouseAdapter);
		list.addMouseMotionListener(dndReorderMouseAdapter);
		
		getRemoveAction().setEnabled(true);
		
		buttonPanel = new JPanel(new MigLayout("insets 1.2mm, nogrid, fill", "align center"));
		
		buttonPanel.add(new JButton(downAction), "gap 10px");
		buttonPanel.add(new JButton(upAction), "gap 0");
		buttonPanel.add(new JButton(loadAction), "gap 10px");
		
		add(buttonPanel, BorderLayout.SOUTH);
		
		listScrollPane.getViewport().setBackground(list.getBackground());
	}
	
	
	public JPanel getButtonPanel() {
		return buttonPanel;
	}
	
	
	@Override
	public void setTransferablePolicy(TransferablePolicy transferablePolicy) {
		super.setTransferablePolicy(transferablePolicy);
		loadAction.putValue(LoadAction.TRANSFERABLE_POLICY, transferablePolicy);
	}
	
	
	private final LoadAction loadAction = new LoadAction(null);
	
	private final AbstractAction upAction = new AbstractAction(null, ResourceManager.getIcon("action.up")) {
		
		public void actionPerformed(ActionEvent e) {
			int index = getListComponent().getSelectedIndex();
			
			if (index > 0) {
				swap(model, index, index - 1);
				getListComponent().setSelectedIndex(index - 1);
			}
		}
	};
	
	private final AbstractAction downAction = new AbstractAction(null, ResourceManager.getIcon("action.down")) {
		
		public void actionPerformed(ActionEvent e) {
			int index = getListComponent().getSelectedIndex();
			
			if (index < model.size() - 1) {
				swap(model, index, index + 1);
				getListComponent().setSelectedIndex(index + 1);
			}
		}
	};
	
	private final MouseAdapter dndReorderMouseAdapter = new MouseAdapter() {
		
		private int lastIndex = -1;
		
		
		@Override
		public void mousePressed(MouseEvent m) {
			lastIndex = getListComponent().getSelectedIndex();
		}
		
		
		@Override
		public void mouseDragged(MouseEvent m) {
			int currentIndex = getListComponent().getSelectedIndex();
			
			if (currentIndex != lastIndex && lastIndex >= 0 && currentIndex >= 0) {
				swap(model, lastIndex, currentIndex);
				lastIndex = currentIndex;
			}
		}
	};
	
}
