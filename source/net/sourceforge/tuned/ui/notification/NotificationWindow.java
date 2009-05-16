/*
 * Created on 19.03.2005
 */

package net.sourceforge.tuned.ui.notification;


import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;

import javax.swing.JWindow;
import javax.swing.Timer;

import net.sourceforge.tuned.ui.TunedUtilities;


public class NotificationWindow extends JWindow {
	
	private final int timeout;
	
	
	public NotificationWindow(Window owner, int timeout) {
		this(owner, timeout, true);
	}
	

	public NotificationWindow(Window owner, int timeout, boolean closeOnClick) {
		super(owner);
		this.timeout = timeout;
		
		setAlwaysOnTop(true);
		
		if (closeOnClick) {
			getGlassPane().addMouseListener(clickListener);
			getGlassPane().setVisible(true);
		}
		
		addComponentListener(closeOnTimeout);
	}
	

	public NotificationWindow(Window owner) {
		this(owner, -1);
	}
	

	public final void close() {
		TunedUtilities.checkEventDispatchThread();
		
		// window events are not fired automatically, required for layout updates
		processWindowEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
		
		setVisible(false);
		
		// component events are not fired automatically, used to cancel timeout timer
		processComponentEvent(new ComponentEvent(this, ComponentEvent.COMPONENT_HIDDEN));
		
		dispose();
	}
	
	private final ComponentListener closeOnTimeout = new ComponentAdapter() {
		
		private Timer timer = null;
		
		
		@Override
		public void componentShown(ComponentEvent e) {
			if (timeout >= 0) {
				timer = TunedUtilities.invokeLater(timeout, new Runnable() {
					
					@Override
					public void run() {
						close();
					}
				});
			}
		}
		

		@Override
		public void componentHidden(ComponentEvent e) {
			if (timer != null) {
				timer.stop();
			}
		}
		
	};
	
	private final MouseAdapter clickListener = new MouseAdapter() {
		
		@Override
		public void mouseClicked(MouseEvent e) {
			close();
		}
	};
	
}
