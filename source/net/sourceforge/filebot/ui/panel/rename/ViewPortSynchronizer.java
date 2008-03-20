
package net.sourceforge.filebot.ui.panel.rename;


import java.awt.Point;

import javax.swing.JViewport;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


class ViewPortSynchronizer {
	
	private JViewport viewport1;
	private JViewport viewport2;
	
	private ViewPortSynchronizeListener viewPortSynchronizeListener1;
	private ViewPortSynchronizeListener viewPortSynchronizeListener2;
	
	
	public ViewPortSynchronizer(JViewport viewport1, JViewport viewport2) {
		this.viewport1 = viewport1;
		this.viewport2 = viewport2;
		
		viewPortSynchronizeListener1 = new ViewPortSynchronizeListener(viewport2);
		viewPortSynchronizeListener2 = new ViewPortSynchronizeListener(viewport1);
		
		setEnabled(true);
	}
	

	public void setEnabled(boolean enabled) {
		// remove listeners to avoid adding them multiple times
		viewport1.removeChangeListener(viewPortSynchronizeListener1);
		viewport2.removeChangeListener(viewPortSynchronizeListener2);
		
		// if enabled add them again
		if (enabled) {
			viewport1.addChangeListener(viewPortSynchronizeListener1);
			viewport2.addChangeListener(viewPortSynchronizeListener2);
		}
	}
	
	
	private static class ViewPortSynchronizeListener implements ChangeListener {
		
		private JViewport target;
		
		
		public ViewPortSynchronizeListener(JViewport target) {
			this.target = target;
		}
		

		@Override
		public void stateChanged(ChangeEvent e) {
			JViewport source = (JViewport) e.getSource();
			
			Point viewPosition = source.getViewPosition();
			
			// return if both viewports have the same view position
			if (viewPosition.equals(target.getViewPosition()))
				return;
			
			target.setViewPosition(viewPosition);
			target.repaint();
		}
	}
	
}
