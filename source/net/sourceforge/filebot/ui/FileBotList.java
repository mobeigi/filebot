
package net.sourceforge.filebot.ui;


import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;

import net.sourceforge.filebot.FileBotUtil;
import net.sourceforge.filebot.ui.transfer.DefaultTransferHandler;
import net.sourceforge.filebot.ui.transfer.ExportHandler;
import net.sourceforge.filebot.ui.transfer.FileTransferable;
import net.sourceforge.filebot.ui.transfer.ImportHandler;
import net.sourceforge.filebot.ui.transfer.Saveable;
import net.sourceforge.filebot.ui.transfer.SaveableExportHandler;
import net.sourceforge.filebot.ui.transfer.TransferablePolicyImportHandler;
import net.sourceforge.filebot.ui.transfer.TransferablePolicySupport;
import net.sourceforge.filebot.ui.transferablepolicies.NullTransferablePolicy;
import net.sourceforge.filebot.ui.transferablepolicies.TransferablePolicy;
import net.sourceforge.tuned.ui.FancyListCellRenderer;
import net.sourceforge.tuned.ui.SimpleListModel;


public class FileBotList extends JPanel implements Saveable, TransferablePolicySupport {
	
	private JList list = new JList(new SimpleListModel());
	
	private TitledBorder titledBorder;
	
	private String title;
	
	private TransferablePolicy transferablePolicy = new NullTransferablePolicy();
	
	
	public FileBotList(boolean enableDrop, boolean enableDrag, boolean enableRemoveAction) {
		this(enableDrop, enableDrag, enableRemoveAction, true);
	}
	

	public FileBotList(boolean enableImport, boolean enableExport, boolean enableRemoveAction, boolean border) {
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
		
		ImportHandler importHander = null;
		ExportHandler exportHandler = null;
		
		if (enableImport)
			importHander = new TransferablePolicyImportHandler(this);
		
		if (enableExport)
			exportHandler = new SaveableExportHandler(this);
		
		list.setTransferHandler(new DefaultTransferHandler(importHander, exportHandler));
		list.setDragEnabled(enableExport);
		
		if (enableRemoveAction) {
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
	

	public SimpleListModel getModel() {
		return (SimpleListModel) list.getModel();
	}
	

	public void save(File file) {
		try {
			PrintStream out = new PrintStream(file);
			
			for (Object object : getModel().getCopy()) {
				out.println(object.toString());
			}
			
			out.close();
		} catch (FileNotFoundException e) {
			// should not happen
			Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, e.toString(), e);
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
				getModel().remove(value);
			
			int maxIndex = list.getModel().getSize() - 1;
			
			if (index > maxIndex)
				index = maxIndex;
			
			list.setSelectedIndex(index);
		}
	};
	
}
