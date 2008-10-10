
package net.sourceforge.filebot.ui.panel.search;


import javax.swing.Icon;
import javax.swing.JComponent;

import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.ui.FileBotList;
import net.sourceforge.filebot.ui.FileBotListExportHandler;
import net.sourceforge.filebot.ui.FileBotTabComponent;
import net.sourceforge.filebot.web.Episode;


public class EpisodeListPanel extends FileBotList<Episode> {
	
	private final FileBotTabComponent tabComponent = new FileBotTabComponent();
	
	private Icon icon;
	
	private boolean loading = false;
	
	
	public EpisodeListPanel() {
		setExportHandler(new FileBotListExportHandler(this));
		getRemoveAction().setEnabled(true);
		
		setBorder(null);
		listScrollPane.setBorder(null);
	}
	

	public JComponent getTabComponent() {
		return tabComponent;
	}
	

	@Override
	public void setTitle(String title) {
		super.setTitle(title);
		tabComponent.setText(title);
	}
	

	public void setIcon(Icon icon) {
		synchronized (tabComponent) {
			this.icon = icon;
			
			if (!loading) {
				tabComponent.setIcon(icon);
			}
		}
	}
	

	public Icon getIcon() {
		return icon;
	}
	

	public void setLoading(boolean loading) {
		synchronized (tabComponent) {
			if (loading) {
				tabComponent.setIcon(ResourceManager.getIcon("tab.loading"));
			} else {
				tabComponent.setIcon(icon);
			}
		}
	}
	
}
