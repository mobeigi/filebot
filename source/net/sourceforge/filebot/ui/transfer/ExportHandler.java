
package net.sourceforge.filebot.ui.transfer;


import java.awt.datatransfer.Transferable;

import javax.swing.JComponent;


public interface ExportHandler {
	
	public abstract void exportDone(JComponent source, Transferable data, int action);
	

	public abstract int getSourceActions(JComponent c);
	

	public abstract Transferable createTransferable(JComponent c);
}
