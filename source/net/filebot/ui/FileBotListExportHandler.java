
package net.filebot.ui;

import java.awt.Cursor;
import java.io.PrintWriter;

import net.filebot.ui.transfer.TextFileExportHandler;

public class FileBotListExportHandler<T> extends TextFileExportHandler {

	protected final FileBotList<T> list;

	public FileBotListExportHandler(FileBotList<T> list) {
		this.list = list;
	}

	@Override
	public boolean canExport() {
		return list.getModel().size() > 0;
	}

	@Override
	public void export(PrintWriter out) {
		try {
			list.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			for (T item : list.getModel()) {
				export(item, out);
			}
		} finally {
			list.setCursor(Cursor.getDefaultCursor());
		}
	}

	public void export(T item, PrintWriter out) {
		out.println(item);
	}

	@Override
	public String getDefaultFileName() {
		return list.getTitle() + ".txt";
	}

}
