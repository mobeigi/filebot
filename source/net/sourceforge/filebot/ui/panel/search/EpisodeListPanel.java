
package net.sourceforge.filebot.ui.panel.search;


import javax.swing.ImageIcon;

import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.filebot.ui.FileBotList;


class EpisodeListPanel extends FileBotList {
	
	private TabComponentWithClose tabComponent;
	private ImageIcon icon;
	
	private boolean loading = false;
	
	
	public EpisodeListPanel() {
		super(false, true, true, false);
		tabComponent = new TabComponentWithClose();
	}
	

	public TabComponentWithClose getTabComponent() {
		return tabComponent;
	}
	

	@Override
	public void setTitle(String title) {
		super.setTitle(title);
		tabComponent.setText(title);
	}
	

	public void setIcon(ImageIcon icon) {
		this.icon = icon;
		
		if (!loading) {
			tabComponent.setIcon(icon);
		}
	}
	

	public ImageIcon getIcon() {
		return icon;
	}
	

	public void setLoading(boolean loading) {
		if (loading) {
			tabComponent.setIcon(ResourceManager.getIcon("tab.loading"));
		} else {
			tabComponent.setIcon(icon);
		}
	}
	
}
