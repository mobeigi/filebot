
package net.sourceforge.filebot.ui;


import java.awt.datatransfer.Transferable;
import java.util.Arrays;

import javax.swing.JComponent;
import javax.swing.JFrame;

import net.miginfocom.swing.MigLayout;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.ui.transfer.TransferablePolicy;
import net.sourceforge.filebot.ui.transfer.TransferablePolicy.TransferAction;
import net.sourceforge.tuned.ExceptionUtilities;


public class SinglePanelFrame extends JFrame {
	
	private final JComponent panel;
	
	
	public SinglePanelFrame(PanelBuilder builder) {
		super(builder.getName());
		
		panel = builder.create();
		
		// set taskbar / taskswitch icons
		setIconImages(Arrays.asList(ResourceManager.getImage("window.icon.small"), ResourceManager.getImage("window.icon.big")));
		
		JComponent c = (JComponent) getContentPane();
		c.setLayout(new MigLayout("insets 0, nogrid, fill", "fill", "fill"));
		
		HeaderPanel headerPanel = new HeaderPanel();
		headerPanel.setTitle(builder.getName());
		
		c.add(headerPanel, "growx, dock north");
		c.add(panel);
		
		setSize(760, 615);
	}
	

	public SinglePanelFrame publish(Transferable transferable) {
		TransferablePolicy policy = (TransferablePolicy) panel.getClientProperty("transferablePolicy");
		
		try {
			if (policy != null && policy.accept(transferable)) {
				policy.handleTransferable(transferable, TransferAction.ADD);
			}
		} catch (Exception e) {
			throw ExceptionUtilities.asRuntimeException(e);
		}
		
		return this;
	}
}
