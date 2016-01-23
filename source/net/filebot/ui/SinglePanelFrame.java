package net.filebot.ui;

import static net.filebot.Settings.*;

import java.awt.Dimension;
import java.awt.datatransfer.Transferable;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.border.EmptyBorder;

import net.filebot.ui.transfer.TransferablePolicy;
import net.filebot.ui.transfer.TransferablePolicy.TransferAction;
import net.filebot.util.ExceptionUtilities;
import net.miginfocom.swing.MigLayout;

public class SinglePanelFrame extends JFrame {

	private final JComponent panel;

	public SinglePanelFrame(PanelBuilder builder) {
		super(String.format("%s %s %s", getApplicationName(), builder.getName(), getApplicationVersion()));
		panel = builder.create();

		JComponent c = (JComponent) getContentPane();
		c.setLayout(new MigLayout("insets 0, nogrid, fill", "fill", "fill"));
		c.add(panel);

		HeaderPanel headerPanel = new HeaderPanel();
		headerPanel.getTitleLabel().setBorder(new EmptyBorder(8, 8, 8, 8));
		headerPanel.getTitleLabel().setIcon(builder.getIcon());
		headerPanel.getTitleLabel().setText(builder.getName());
		headerPanel.getTitleLabel().setIconTextGap(15);
		c.add(headerPanel, "growx, dock north");

		setSize(850, 600);
		setMinimumSize(new Dimension(800, 400));
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
