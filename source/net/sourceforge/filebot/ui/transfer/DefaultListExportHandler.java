
package net.sourceforge.filebot.ui.transfer;


import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.swing.JList;
import javax.swing.ListModel;


public class DefaultListExportHandler extends FileExportHandler {
	
	private final JList list;
	
	
	public DefaultListExportHandler(JList list) {
		this.list = list;
	}
	

	@Override
	public boolean canExport() {
		return list.getModel().getSize() > 0;
	}
	

	@Override
	public void export(OutputStream out) throws IOException {
		PrintStream printer = new PrintStream(out);
		
		ListModel model = list.getModel();
		
		for (int i = 0; i < model.getSize(); i++) {
			printer.println(model.getElementAt(i));
		}
	}
	

	@Override
	public String getDefaultFileName() {
		return list.getClientProperty("title") + ".txt";
	}
}
