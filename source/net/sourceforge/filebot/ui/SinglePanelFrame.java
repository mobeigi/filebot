
package net.sourceforge.filebot.ui;


import java.awt.Image;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.List;

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
		List<Image> images = new ArrayList<Image>(3);
		for (String i : new String[] { "window.icon.large", "window.icon.medium", "window.icon.small" }) {
			images.add(ResourceManager.getImage(i));
		}
		setIconImages(images);
		
		JComponent c = (JComponent) getContentPane();
		c.setLayout(new MigLayout("insets 0, nogrid, fill", "fill", "fill"));
		c.add(panel);
		
		setSize(760, 480);
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
