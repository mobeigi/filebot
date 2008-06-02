
package net.sourceforge.filebot.ui.panel.subtitle;


import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import net.sourceforge.filebot.web.SubtitleDescriptor;
import net.sourceforge.tuned.ui.SimpleListModel;


public class SubtitleDownloadPanel extends JPanel {
	
	private final SubtitlePackagePanel packagePanel = new SubtitlePackagePanel();
	
	
	public SubtitleDownloadPanel() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		add(packagePanel);
	}
	

	public SubtitlePackagePanel getPackagePanel() {
		return packagePanel;
	}
	

	public void addSubtitleDescriptors(List<? extends SubtitleDescriptor> subtitleDescriptors) {
		SimpleListModel model = new SimpleListModel();
		
		for (SubtitleDescriptor subtitleDescriptor : subtitleDescriptors) {
			model.add(new SubtitlePackage(subtitleDescriptor));
		}
		//TODO real add, not setModel
		packagePanel.setModel(model);
	}
	
}
