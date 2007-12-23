
package net.sourceforge.filebot.ui.panel.analyze.tools;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Collection;

import javax.swing.JComponent;

import net.sourceforge.tuned.ui.LoadingOverlayPanel;


public abstract class ToolPanel extends JComponent {
	
	private String name = null;
	
	private LoadingOverlayPanel loadingOverlay;
	
	public static final String LOADING_PROPERTY = "loading";
	
	
	public ToolPanel(String name) {
		this.name = name;
		addPropertyChangeListener(LOADING_PROPERTY, loadingOverlayUpdateListener);
	}
	

	@Override
	public String getName() {
		return name;
	}
	

	protected void setLoadingOverlayPane(LoadingOverlayPanel c) {
		loadingOverlay = c;
	}
	
	private PropertyChangeListener loadingOverlayUpdateListener = new PropertyChangeListener() {
		
		public void propertyChange(PropertyChangeEvent evt) {
			Boolean loading = (Boolean) evt.getNewValue();
			
			if (loadingOverlay == null)
				return;
			
			loadingOverlay.setOverlayVisible(loading);
			loadingOverlay.updateOverlayUI();
		}
	};
	
	
	public abstract void update(Collection<File> list);
}
