/*
 * Created on 19.03.2005
 *
 */

package net.sourceforge.tuned.ui.notification;


import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JWindow;
import javax.swing.Timer;

import net.sourceforge.tuned.ui.TunedUtil;


public class NotificationWindow extends JWindow {
	
	private int timeout;
	
	
	public NotificationWindow(Window owner, int timeout) {
		this(owner, timeout, true);
	}
	

	public NotificationWindow(Window owner, int timeout, boolean closeOnClick) {
		super(owner);
		this.timeout = timeout;
		
		setAlwaysOnTop(true);
		
		if (closeOnClick)
			getGlassPane().addMouseListener(clickListener);
		
		getGlassPane().setVisible(true);
		
		addComponentListener(visibleListener);
	}
	

	public NotificationWindow(int timeout) {
		this((Window) null, timeout);
	}
	

	public NotificationWindow(Window owner) {
		this(owner, -1);
	}
	

	public NotificationWindow() {
		this((Window) null, -1);
	}
	

	public final void close() {
		setVisible(false);
		
		// component hidden is not fired automatically
		processComponentEvent(new ComponentEvent(this, ComponentEvent.COMPONENT_HIDDEN));
	}
	
	private ComponentListener visibleListener = new ComponentAdapter() {
		
		private Timer timer;
		
		
		@Override
		public void componentShown(ComponentEvent e) {
			if (timeout >= 0) {
				timer = TunedUtil.invokeLater(timeout, new Runnable() {
					
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
	
	private MouseAdapter clickListener = new MouseAdapter() {
		
		@Override
		public void mouseClicked(MouseEvent e) {
			close();
		}
	};
	
}
