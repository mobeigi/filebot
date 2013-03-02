package net.sourceforge.tuned.ui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import net.miginfocom.swing.MigLayout;

public class ActionPopup extends JPopupMenu {

	protected final JLabel headerLabel = new JLabel();
	protected final JLabel descriptionLabel = new JLabel();
	protected final JLabel statusLabel = new JLabel();

	protected final JPanel actionPanel = new JPanel(new MigLayout("nogrid, insets 0, fill"));

	public ActionPopup(String label, Icon icon) {
		headerLabel.setText(label);
		headerLabel.setIcon(icon);
		headerLabel.setIconTextGap(5);

		actionPanel.setOpaque(false);

		statusLabel.setFont(statusLabel.getFont().deriveFont(10f));
		statusLabel.setForeground(Color.GRAY);

		setLayout(new MigLayout("nogrid, fill, insets 0"));

		add(headerLabel, "gapx 5px 5px, gapy 3px 1px, wrap 3px");
		add(new JSeparator(), "growx, wrap 1px");
		add(actionPanel, "growx, wrap 0px");
		add(new JSeparator(), "growx, wrap 0px");
		add(statusLabel, "growx, h 11px!, gapx 3px, wrap 1px");

		// make it look better (e.g. window shadows) by forcing heavy-weight windows
		setLightWeightPopupEnabled(false);
	}

	public void addDescription(JComponent component) {
		actionPanel.add(component, "gapx 4px, wrap 3px");
	}

	public void addAction(JComponent component) {
		actionPanel.add(component, "gapx 12px 12px, growx, wrap");
	}

	@Override
	public void addSeparator() {
		actionPanel.add(new JSeparator(), "growx, wrap 1px");
	}

	@Override
	public JMenuItem add(Action a) {
		LinkButton link = new LinkButton(a);

		// underline text
		link.setText(String.format("<html><nobr><u>%s</u></nobr></html>", link.getText()));

		// use rollover color
		link.setRolloverEnabled(false);
		link.setColor(link.getRolloverColor());

		// close popup when action is triggered
		link.addActionListener(closeListener);

		addAction(link);
		return null;
	}

	public void clear() {
		actionPanel.removeAll();
	}

	@Override
	public void setLabel(String label) {
		headerLabel.setText(label);
	}

	@Override
	public String getLabel() {
		return headerLabel.getText();
	}

	public void setStatus(String string) {
		statusLabel.setText(string);
	}

	public String getStatus() {
		return statusLabel.getText();
	}

	private final ActionListener closeListener = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
			setVisible(false);
		}
	};

}
