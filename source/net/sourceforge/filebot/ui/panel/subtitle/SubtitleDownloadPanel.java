
package net.sourceforge.filebot.ui.panel.subtitle;


import java.awt.BorderLayout;

import javax.swing.JComponent;


public class SubtitleDownloadPanel extends JComponent {
	
	private final SubtitlePackagePanel packagePanel = new SubtitlePackagePanel();
	
	
	public SubtitleDownloadPanel() {
		setLayout(new BorderLayout());
		
		add(packagePanel, BorderLayout.CENTER);
	}
	

	public SubtitlePackagePanel getPackagePanel() {
		return packagePanel;
	}
	
}
