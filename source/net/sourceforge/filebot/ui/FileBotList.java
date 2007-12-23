
package net.sourceforge.filebot.ui;


import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;

import net.sourceforge.filebot.ui.sal.FileTransferable;
import net.sourceforge.filebot.ui.sal.Saveable;
import net.sourceforge.filebot.ui.transferablepolicies.NullTransferablePolicy;
import net.sourceforge.filebot.ui.transferablepolicies.TransferablePolicy;
import net.sourceforge.filebot.ui.transferablepolicies.TransferablePolicySupport;
import net.sourceforge.tuned.ui.FancyListCellRenderer;


public class FileBotList extends JPanel implements Saveable, TransferablePolicySupport {
	
	private JList list = new JList(new DefaultListModel());
	
	private TitledBorder titledBorder;
	
	private String title;
	
	private TransferablePolicy transferablePolicy = new NullTransferablePolicy();
	
	
	public FileBotList(boolean enableDrop, boolean enableDrag, boolean initRemoveAction) {
		this(enableDrop, enableDrag, initRemoveAction, true);
	}
	

	public FileBotList(boolean enableDrop, boolean enableDrag, boolean initRemoveAction, boolean border) {
		super(new BorderLayout());
		
		JScrollPane listScrollPane = new JScrollPane(list);
		
		if (border) {
			titledBorder = new TitledBorder("");
			setBorder(titledBorder);
		} else {
			listScrollPane.setBorder(BorderFactory.createEmptyBorder());
		}
		
		list.setCellRenderer(new FancyListCellRenderer());
		list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		add(listScrollPane, BorderLayout.CENTER);
		
		TransferablePolicySupport handlerTransferablePolicySupport = null;
		Saveable handlerSaveable = null;
		
		if (enableDrop) {
			handlerTransferablePolicySupport = this;
		}
		
		if (enableDrag) {
			handlerSaveable = this;
		}
		
		list.setTransferHandler(new FileBotTransferHandler(handlerTransferablePolicySupport, handlerSaveable));
		
		if (handlerSaveable != null)
			MouseDragRecognizeListener.createForComponent(this.getListComponent());
		
		if (initRemoveAction) {
			// Shortcut DELETE
			FileBotUtil.registerActionForKeystroke(this, KeyStroke.getKeyStroke("pressed DELETE"), removeAction);
		}
	}
	

	public JList getListComponent() {
		return list;
	}
	

	public void setTransferablePolicy(TransferablePolicy transferablePolicy) {
		this.transferablePolicy = transferablePolicy;
	}
	

	public TransferablePolicy getTransferablePolicy() {
		return transferablePolicy;
	}
	

	public String getTitle() {
		return title;
	}
	

	public void setTitle(String title) {
		this.title = title;
		
		if (titledBorder != null)
			titledBorder.setTitle(title);
		
		if (isVisible()) {
			revalidate();
			repaint();
		}
	}
	

	public DefaultListModel getModel() {
		return (DefaultListModel) list.getModel();
	}
	

	public void save(File file) {
		try {
			PrintStream out = new PrintStream(file);
			
			DefaultListModel model = getModel();
			
			for (int i = 0; i < model.getSize(); ++i)
				out.println(model.get(i).toString());
			
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	

	public String getDefaultFileName() {
		return title + ".txt";
	}
	

	public boolean isSaveable() {
		return !getModel().isEmpty();
	}
	

	public void load(List<File> files) {
		FileTransferable tr = new FileTransferable(files);
		
		if (transferablePolicy.accept(tr))
			transferablePolicy.handleTransferable(tr, false);
	}
	
	private final AbstractAction removeAction = new AbstractAction("Remove") {
		
		public void actionPerformed(ActionEvent e) {
			int index = list.getSelectedIndex();
			Object values[] = list.getSelectedValues();
			
			for (Object value : values)
				getModel().removeElement(value);
			
			int maxIndex = list.getModel().getSize() - 1;
			
			if (index > maxIndex)
				index = maxIndex;
			
			list.setSelectedIndex(index);
		}
	};
	
}
