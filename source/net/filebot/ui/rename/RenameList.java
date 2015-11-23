package net.filebot.ui.rename;

import static java.util.Collections.*;
import static net.filebot.util.ui.SwingUI.*;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import net.filebot.ResourceManager;
import net.filebot.ui.FileBotList;
import net.filebot.ui.transfer.LoadAction;
import net.filebot.ui.transfer.TransferablePolicy;
import net.filebot.util.ui.ActionPopup;
import net.miginfocom.swing.MigLayout;
import ca.odell.glazedlists.EventList;

class RenameList<E> extends FileBotList<E> {

	private JPanel buttonPanel;

	public RenameList(EventList<E> model) {
		// replace default model with given model
		setModel(model);

		// disable multi-selection for the sake of simplicity
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// need a fixed cell size for high performance scrolling
		list.setFixedCellHeight(28);
		list.getModel().addListDataListener(new ListDataListener() {

			private int longestItemLength = -1;

			@Override
			public void intervalRemoved(ListDataEvent evt) {
				// reset prototype value
				ListModel<?> m = (ListModel<?>) evt.getSource();
				if (m.getSize() == 0) {
					longestItemLength = -1;
					list.setPrototypeCellValue(null);
				}
			}

			@Override
			public void intervalAdded(ListDataEvent evt) {
				contentsChanged(evt);
			}

			@Override
			public void contentsChanged(ListDataEvent evt) {
				ListModel<?> m = (ListModel<?>) evt.getSource();
				for (int i = evt.getIndex0(); i <= evt.getIndex1() && i < m.getSize(); i++) {
					Object item = m.getElementAt(i);
					int itemLength = item.toString().length();
					if (itemLength > longestItemLength) {
						// cell values will not be updated if the prototype object remains the same (even if the object has changed) so we need to reset it
						if (item == list.getPrototypeCellValue()) {
							list.setPrototypeCellValue("");
						}

						longestItemLength = itemLength;
						list.setPrototypeCellValue(item);
					}
				}
			}
		});

		list.addMouseListener(dndReorderMouseAdapter);
		list.addMouseMotionListener(dndReorderMouseAdapter);

		getRemoveAction().setEnabled(true);

		buttonPanel = new JPanel(new MigLayout("insets 1.2mm, nogrid, fill", "align center"));
		buttonPanel.add(createImageButton(downAction), "gap 10px");
		buttonPanel.add(createImageButton(upAction), "gap 0");
		buttonPanel.add(createLoadButton(), "gap 10px");

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

	private JButton createLoadButton() {
		ActionPopup actionPopup = new ActionPopup("Load Files", ResourceManager.getIcon("action.load"));
		actionPopup.add(new AbstractAction("Select Folders", ResourceManager.getIcon("tree.closed")) {

			@Override
			public void actionPerformed(ActionEvent evt) {
				loadAction.actionPerformed(new ActionEvent(evt.getSource(), evt.getID(), evt.getActionCommand(), 0));
			}
		});
		actionPopup.add(new AbstractAction("Select Files", ResourceManager.getIcon("file.generic")) {

			@Override
			public void actionPerformed(ActionEvent evt) {
				loadAction.actionPerformed(new ActionEvent(evt.getSource(), evt.getID(), evt.getActionCommand(), ActionEvent.SHIFT_MASK));
			}
		});

		JButton b = new JButton(loadAction);
		b.setComponentPopupMenu(actionPopup);
		return b;
	}

	private final LoadAction loadAction = new LoadAction(null);

	private final AbstractAction upAction = new AbstractAction("Align Up", ResourceManager.getIcon("action.up")) {

		@Override
		public void actionPerformed(ActionEvent e) {
			int index = getListComponent().getSelectedIndex();

			if (index > 0) {
				swap(model, index, index - 1);
				getListComponent().setSelectedIndex(index - 1);
			}
		}
	};

	private final AbstractAction downAction = new AbstractAction("Align Down", ResourceManager.getIcon("action.down")) {

		@Override
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
