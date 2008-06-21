
package net.sourceforge.filebot.ui.panel.subtitle;


import java.awt.BorderLayout;

import javax.swing.JPanel;


public class SubtitleDownloadPanel extends JPanel {
	
	private final SubtitlePackagePanel packagePanel = new SubtitlePackagePanel();
	
	
	public SubtitleDownloadPanel() {
		setLayout(new BorderLayout());
		
		add(packagePanel, BorderLayout.CENTER);
	}
	

	public SubtitlePackagePanel getPackagePanel() {
		return packagePanel;
	}
	
}
