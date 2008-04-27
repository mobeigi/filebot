
package net.sourceforge.tuned.ui;


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
	
	private final JLabel loadingLabel;
	
	private boolean overlayEnabled = false;
	
	private int millisToOverlay = 500;
	
	private final JComponent view;
	
	
	public LoadingOverlayPane(JComponent component, Icon animation) {
		this(component, animation, getView(component));
	}
	

	public LoadingOverlayPane(JComponent component, Icon animation, JComponent view) {
		this.view = view;
		
		setLayout(new OverlayLayout(this));
		
		component.setAlignmentX(1.0f);
		component.setAlignmentY(0.0f);
		
		loadingLabel = new JLabel(animation);
		loadingLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 20));
		
		loadingLabel.setAlignmentX(1.0f);
		loadingLabel.setAlignmentY(0.0f);
		loadingLabel.setMaximumSize(loadingLabel.getPreferredSize());
		
		add(loadingLabel);
		add(component);
		
		setOverlayVisible(false);
		
		view.addPropertyChangeListener(LOADING_PROPERTY, loadingListener);
	}
	

	private static JComponent getView(JComponent component) {
		if (component instanceof JScrollPane) {
			JScrollPane scrollPane = (JScrollPane) component;
			return (JComponent) scrollPane.getViewport().getView();
		}
		
		return component;
	}
	

	public JComponent getView() {
		return view;
	}
	

	public void setOverlayVisible(boolean b) {
		overlayEnabled = b;
		
		if (overlayEnabled) {
			TunedUtil.invokeLater(millisToOverlay, new Runnable() {
				
				@Override
				public void run() {
					if (overlayEnabled) {
						loadingLabel.setVisible(true);
					}
				}
				
			});
		} else {
			loadingLabel.setVisible(false);
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
