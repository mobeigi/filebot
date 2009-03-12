
package net.sourceforge.filebot.ui;


import java.util.Formatter;

import net.sourceforge.filebot.ui.transfer.TextFileExportHandler;


public class FileBotListExportHandler extends TextFileExportHandler {
	
	protected final FileBotList<?> list;
	
	
	public FileBotListExportHandler(FileBotList<?> list) {
		this.list = list;
	}
	

	@Override
	public boolean canExport() {
		return list.getModel().size() > 0;
	}
	

	@Override
	public void export(Formatter out) {
		for (Object entry : list.getModel()) {
			out.format("%s%n", entry);
		}
	}
	

	@Override
	public String getDefaultFileName() {
		return list.getTitle() + ".txt";
	}
	
}
