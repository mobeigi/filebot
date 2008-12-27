
package net.sourceforge.filebot.ui;


import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.tuned.ui.ProgressIndicator;


public class FileBotTabComponent extends JComponent {
	
	private ProgressIndicator progressIndicator = new ProgressIndicator();
	private JLabel label = new JLabel();
	private JButton closeButton = createCloseButton();
	
	private Icon icon = null;
	private boolean loading = false;
	
	
	public FileBotTabComponent() {
		setLayout(new MigLayout("nogrid, fill, insets 0"));
		
		progressIndicator.setVisible(loading);
		
		add(progressIndicator, "gap right 4px, w 17px!, h 17px!, hidemode 3");
		add(label, "grow");
		add(closeButton, "gap 3px:push, w 17!, h 17!");
	}
	

	public void setLoading(boolean loading) {
		this.loading = loading;
		progressIndicator.setVisible(loading);
		label.setIcon(loading ? null : icon);
	}
	

	public boolean isLoading() {
		return loading;
	}
	

	public void setIcon(Icon icon) {
		this.icon = icon;
		label.setIcon(loading ? null : icon);
	}
	

	public Icon getIcon() {
		return icon;
	}
	

	public void setText(String text) {
		label.setText(text);
	}
	

	public String getText() {
		return label.getText();
	}
	

	public JButton getCloseButton() {
		return closeButton;
	}
	

	private JButton createCloseButton() {
		JButton button = new JButton();
		
		button.setContentAreaFilled(false);
		button.setBorderPainted(false);
		button.setFocusable(false);
		button.setRolloverEnabled(true);
		
		button.setIcon(ResourceManager.getIcon("tab.close"));
		button.setRolloverIcon(ResourceManager.getIcon("tab.close.hover"));
		
		return button;
	}
	
}
