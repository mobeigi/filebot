
package net.sourceforge.tuned.ui;


import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import net.miginfocom.swing.MigLayout;


public class ActionPopup extends JPopupMenu {
	
	protected JLabel headerLabel = new JLabel();
	protected JLabel descriptionLabel = new JLabel();
	protected JLabel statusLabel = new JLabel();
	
	protected JPanel actionPanel = new JPanel(new MigLayout("insets 0, wrap 1"));
	
	
	public ActionPopup(String label, Icon icon) {
		headerLabel.setText(label);
		headerLabel.setIcon(icon);
		headerLabel.setIconTextGap(5);
		
		statusLabel.setFont(statusLabel.getFont().deriveFont(10f));
		statusLabel.setForeground(Color.GRAY);
		
		actionPanel.setOpaque(false);
		
		setLayout(new MigLayout("nogrid, fill, insets 0"));
		
		add(headerLabel, "gapx 5px 5px, gapy 3px 1px, wrap 3px");
		add(new JSeparator(), "growx, wrap 1px");
		add(descriptionLabel, "gapx 4px, wrap 3px");
		add(actionPanel, "gapx 12px 12px, wrap");
		add(new JSeparator(), "growx, wrap 0px");
		add(statusLabel, "growx, h 11px!, gapx 3px, wrap 1px");
	}
	

	@Override
	public JMenuItem add(Action a) {
		LinkButton link = new LinkButton(a);
		
		// close popup when action is triggered
		link.addActionListener(closeListener);
		
		// underline text
		link.setText(String.format("<html><u>%s</u></html>", link.getText()));
		
		// use rollover color
		link.setRolloverEnabled(false);
		link.setColor(link.getRolloverColor());
		
		actionPanel.add(link);
		
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
	

	public void setDescription(String string) {
		descriptionLabel.setText(string);
	}
	

	public String getDescription() {
		return descriptionLabel.getText();
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
