
package net.sourceforge.filebot.ui;


import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import net.sourceforge.filebot.ui.transfer.FileExportHandler;


public class FileBotListExportHandler extends FileExportHandler {
	
	private final FileBotList<?> list;
	
	
	public FileBotListExportHandler(FileBotList<?> list) {
		this.list = list;
	}
	

	@Override
	public boolean canExport() {
		return !list.getModel().isEmpty();
	}
	

	@Override
	public void export(OutputStream out) throws IOException {
		PrintStream printer = new PrintStream(out);
		
		for (Object entry : list.getModel()) {
			printer.println(entry);
		}
	}
	

	@Override
	public String getDefaultFileName() {
		return list.getTitle() + ".txt";
	}
	
}
