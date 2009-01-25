
package net.sourceforge.filebot.ui;


import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;

import net.sourceforge.filebot.ui.transfer.DefaultTransferHandler;
import net.sourceforge.filebot.ui.transfer.TextFileExportHandler;
import net.sourceforge.filebot.ui.transfer.TransferablePolicy;
import net.sourceforge.tuned.ui.DefaultFancyListCellRenderer;
import net.sourceforge.tuned.ui.TunedUtilities;
import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.EventListModel;


public class FileBotList<E> extends JComponent {
	
	protected EventList<E> model = new BasicEventList<E>();
	
	protected JList list = new JList(new EventListModel<E>(model));
	
	protected JScrollPane listScrollPane = new JScrollPane(list);
	
	private String title = null;
	
	
	public FileBotList() {
		setLayout(new BorderLayout());
		setBorder(new TitledBorder(getTitle()));
		
		list.setCellRenderer(new DefaultFancyListCellRenderer());
		list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		
		list.setTransferHandler(new DefaultTransferHandler(null, null));
		list.setDragEnabled(false);
		
		add(listScrollPane, BorderLayout.CENTER);
		
		// Shortcut DELETE, disabled by default
		removeAction.setEnabled(false);
		
		TunedUtilities.putActionForKeystroke(this, KeyStroke.getKeyStroke("pressed DELETE"), removeAction);
	}
	

	public EventList<E> getModel() {
		return model;
	}
	

	public void setModel(EventList<E> model) {
		this.model = model;
		list.setModel(new EventListModel<E>(model));
	}
	

	public JList getListComponent() {
		return list;
	}
	

	@Override
	public DefaultTransferHandler getTransferHandler() {
		return (DefaultTransferHandler) list.getTransferHandler();
	}
	

	public void setTransferablePolicy(TransferablePolicy transferablePolicy) {
		getTransferHandler().setTransferablePolicy(transferablePolicy);
	}
	

	public TransferablePolicy getTransferablePolicy() {
		return getTransferHandler().getTransferablePolicy();
	}
	

	public void setExportHandler(TextFileExportHandler exportHandler) {
		getTransferHandler().setExportHandler(exportHandler);
		
		// enable drag if export handler is available
		list.setDragEnabled(exportHandler != null);
	}
	

	public TextFileExportHandler getExportHandler() {
		return (TextFileExportHandler) getTransferHandler().getExportHandler();
	}
	

	public String getTitle() {
		return title;
	}
	

	public void setTitle(String title) {
		this.title = title;
		
		if (getBorder() instanceof TitledBorder) {
			TitledBorder titledBorder = (TitledBorder) getBorder();
			titledBorder.setTitle(title);
			
			revalidate();
			repaint();
		}
	}
	

	public Action getRemoveAction() {
		return removeAction;
	}
	
	private final AbstractAction removeAction = new AbstractAction("Remove") {
		
		public void actionPerformed(ActionEvent e) {
			int index = list.getSelectedIndex();
			Object values[] = list.getSelectedValues();
			
			for (Object value : values)
				getModel().remove(value);
			
			int maxIndex = list.getModel().getSize() - 1;
			
			if (index > maxIndex)
				index = maxIndex;
			
			list.setSelectedIndex(index);
		}
	};
	
}
