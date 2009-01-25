
package net.sourceforge.filebot.ui;


import java.awt.Dimension;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.tuned.ui.ProgressIndicator;
import net.sourceforge.tuned.ui.TunedUtilities;


public class FileBotTabComponent extends JComponent {
	
	private ProgressIndicator progressIndicator = new ProgressIndicator();
	private JLabel textLabel = new JLabel();
	private JLabel iconLabel = new JLabel();
	private AbstractButton closeButton = createCloseButton();
	
	private boolean loading = false;
	
	
	public FileBotTabComponent() {
		iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
		textLabel.setHorizontalAlignment(SwingConstants.LEFT);
		
		progressIndicator.setVisible(loading);
		progressIndicator.setMinimumSize(new Dimension(16, 16));
		
		setLayout(new MigLayout("nogrid, insets 0 0 1 3"));
		
		add(progressIndicator, "hidemode 3");
		add(iconLabel, "hidemode 3");
		add(textLabel, "gap rel, align left");
		add(closeButton, "gap unrel:push, hidemode 3, align center 45%");
	}
	

	public void setLoading(boolean loading) {
		this.loading = loading;
		progressIndicator.setVisible(loading);
		iconLabel.setVisible(!loading);
	}
	

	public boolean isLoading() {
		return loading;
	}
	

	public void setIcon(Icon icon) {
		iconLabel.setIcon(icon);
		progressIndicator.setPreferredSize(icon != null ? TunedUtilities.getDimension(icon) : progressIndicator.getMinimumSize());
	}
	

	public Icon getIcon() {
		return iconLabel.getIcon();
	}
	

	public void setText(String text) {
		textLabel.setText(text);
	}
	

	public String getText() {
		return textLabel.getText();
	}
	

	public AbstractButton getCloseButton() {
		return closeButton;
	}
	

	protected AbstractButton createCloseButton() {
		Icon icon = ResourceManager.getIcon("tab.close");
		Icon rolloverIcon = ResourceManager.getIcon("tab.close.hover");
		
		JButton button = new JButton(icon);
		button.setRolloverIcon(rolloverIcon);
		
		button.setPreferredSize(TunedUtilities.getDimension(rolloverIcon));
		button.setMaximumSize(button.getPreferredSize());
		
		button.setContentAreaFilled(false);
		button.setBorderPainted(false);
		button.setFocusable(false);
		button.setRolloverEnabled(true);
		
		return button;
	}
}
