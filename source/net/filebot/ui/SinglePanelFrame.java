package net.filebot.ui;

import java.awt.datatransfer.Transferable;

import javax.swing.JComponent;
import javax.swing.JFrame;

import net.filebot.ui.transfer.TransferablePolicy;
import net.filebot.ui.transfer.TransferablePolicy.TransferAction;
import net.filebot.util.ExceptionUtilities;
import net.miginfocom.swing.MigLayout;

public class SinglePanelFrame extends JFrame {

	private final JComponent panel;

	public SinglePanelFrame(PanelBuilder builder) {
		super(builder.getName());
		panel = builder.create();

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
