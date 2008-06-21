
package net.sourceforge.tuned.ui;


import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.OverlayLayout;


public class LoadingOverlayPane extends JComponent {
	
	public static final String LOADING_PROPERTY = "loading";
	
	private final JComponent animationComponent;
	
	private boolean overlayEnabled = false;
	
	private int millisToOverlay = 500;
	
	
	public LoadingOverlayPane(JComponent component, Icon animation) {
		this(component, new JLabel(""), getView(component));
	}
	

	public LoadingOverlayPane(JComponent component, JComponent animation) {
		this(component, animation, getView(component));
	}
	

	public LoadingOverlayPane(JComponent component, JComponent animation, JComponent view) {
		setLayout(new OverlayLayout(this));
		
		this.animationComponent = animation;
		
		component.setAlignmentX(1.0f);
		component.setAlignmentY(0.0f);
		
		animation.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 20));
		
		animation.setAlignmentX(1.0f);
		animation.setAlignmentY(0.0f);
		animationComponent.setPreferredSize(new Dimension(48, 48));
		animationComponent.setMaximumSize(animationComponent.getPreferredSize());
		
		add(animation);
		add(component);
		
		setOverlayVisible(true);
		
		view.addPropertyChangeListener(LOADING_PROPERTY, loadingListener);
	}
	

	@Override
	public boolean isOptimizedDrawingEnabled() {
		return false;
	}
	

	private static JComponent getView(JComponent component) {
		if (component instanceof JScrollPane) {
			JScrollPane scrollPane = (JScrollPane) component;
			return (JComponent) scrollPane.getViewport().getView();
		}
		
		return component;
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
