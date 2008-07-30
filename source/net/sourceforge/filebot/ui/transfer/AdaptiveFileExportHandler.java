
package net.sourceforge.filebot.ui.transfer;


import java.io.IOException;
import java.io.OutputStream;


public abstract class AdaptiveFileExportHandler extends FileExportHandler {
	
	/**
	 * @return the <code>FileExportHandler</code> that that should be used, or
	 *         <code>null</code> if export is not possible in the first place
	 */
	protected abstract FileExportHandler getExportHandler();
	

	@Override
	public boolean canExport() {
		FileExportHandler handler = getExportHandler();
		
		if (handler == null)
			return false;
		
		return handler.canExport();
	}
	

	@Override
	public void export(OutputStream out) throws IOException {
		getExportHandler().export(out);
	}
	

	@Override
	public String getDefaultFileName() {
		return getExportHandler().getDefaultFileName();
	}
	
}
