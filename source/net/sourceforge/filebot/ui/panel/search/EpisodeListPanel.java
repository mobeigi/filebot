
package net.sourceforge.filebot.ui.panel.search;


import javax.swing.Icon;
import javax.swing.JComponent;

import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.filebot.ui.FileBotList;
import net.sourceforge.filebot.ui.FileBotTabComponent;


public class EpisodeListPanel extends FileBotList {
	
	private final FileBotTabComponent tabComponent = new FileBotTabComponent();
	
	private Icon icon;
	
	private boolean loading = false;
	
	
	public EpisodeListPanel() {
		super(true, true, false);
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
