
package net.sourceforge.tuned.ui;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;

import net.miginfocom.swing.MigLayout;


public class LoadingOverlayPane extends JComponent {
	
	public static final String LOADING_PROPERTY = "loading";
	
	private final JComponent animationComponent;
	
	private boolean overlayEnabled = false;
	
	private int millisToOverlay = 400;
	
	
	public LoadingOverlayPane(JComponent component, JComponent propertyChangeSource) {
		this(component, new ProgressIndicator(), propertyChangeSource);
	}
	

	public LoadingOverlayPane(JComponent component, JComponent animationComponent, JComponent propertyChangeSource) {
		setLayout(new MigLayout("insets 0, fill"));
		this.animationComponent = animationComponent;
		
		add(animationComponent, "pos n 8px 100%-18px n");
		add(component, "grow");
		
		animationComponent.setVisible(false);
		
		if (propertyChangeSource != null) {
			propertyChangeSource.addPropertyChangeListener(LOADING_PROPERTY, loadingListener);
		}
	}
	

	@Override
	public boolean isOptimizedDrawingEnabled() {
		return false;
	}
	

	public void setOverlayVisible(boolean b) {
		overlayEnabled = b;
		
		if (overlayEnabled) {
			TunedUtil.invokeLater(millisToOverlay, new Runnable() {
				
				@Override
				public void run() {
					if (overlayEnabled) {
						animationComponent.setVisible(true);
					}
				}
				
			});
		} else {
			animationComponent.setVisible(false);
		}
	}
	
	private final PropertyChangeListener loadingListener = new PropertyChangeListener() {
		
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			Boolean loading = (Boolean) evt.getNewValue();
			
			setOverlayVisible(loading);
		}
		
	};
	
}
