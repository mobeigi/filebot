
package net.sourceforge.tuned.ui;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.OverlayLayout;
import javax.swing.Timer;


public class LoadingOverlayPanel extends JPanel {
	
	private JLabel loadingLabel = new JLabel();
	
	private boolean overlayEnabled = false;
	
	private int millisToOverlay = 500;
	
	
	public LoadingOverlayPanel(JComponent component, Icon animation) {
		setLayout(new OverlayLayout(this));
		
		component.setAlignmentX(1.0f);
		component.setAlignmentY(0.0f);
		
		loadingLabel.setIcon(animation);
		loadingLabel.setOpaque(false);
		loadingLabel.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 16));
		
		loadingLabel.setAlignmentX(1.0f);
		loadingLabel.setAlignmentY(0.0f);
		loadingLabel.setMaximumSize(loadingLabel.getPreferredSize());
		
		add(loadingLabel);
		add(component);
		
		setOverlayVisible(false);
	}
	

	public void setMillisToOverlay(int millisToOverlay) {
		this.millisToOverlay = millisToOverlay;
	}
	

	public int getMillisToOverlay() {
		return millisToOverlay;
	}
	

	public void setOverlayVisible(boolean b) {
		overlayEnabled = b;
		
		if (overlayEnabled) {
			new EnableOverlayTimer().start();
		} else {
			loadingLabel.setVisible(false);
		}
	}
	
	
	private class EnableOverlayTimer extends Timer implements ActionListener {
		
		public EnableOverlayTimer() {
			super(millisToOverlay, null);
			addActionListener(this);
			setRepeats(false);
		}
		

		public void actionPerformed(ActionEvent e) {
			if (overlayEnabled) {
				loadingLabel.setVisible(true);
			}
		}
	}
	
	
	public void updateOverlayUI() {
		loadingLabel.updateUI();
	}
	
}
