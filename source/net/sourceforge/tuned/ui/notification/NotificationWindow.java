/*
 * Created on 19.03.2005
 *
 */

package net.sourceforge.tuned.ui.notification;


import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.Timer;


/**
 * @author Reinhard
 * 
 */
public class NotificationWindow extends JWindow {
	
	private Timer timer;
	
	
	public NotificationWindow(Window owner, int timeout) {
		this(owner, timeout, true);
	}
	

	public NotificationWindow(Window owner, int timeout, boolean closeOnClick) {
		super(owner);
		setAlwaysOnTop(true);
		
		if (closeOnClick)
			getGlassPane().addMouseListener(clickListener);
		
		getGlassPane().setVisible(true);
		
		if (timeout >= 0) {
			ActionListener doClose = new ActionListener() {
				
				public void actionPerformed(ActionEvent e) {
					close();
				}
			};
			
			timer = new Timer(timeout, doClose);
			timer.setRepeats(false);
			
			addWindowListener(windowListener);
		}
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
		processWindowEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
		
		SwingUtilities.invokeLater(new Runnable() {
			
			public void run() {
				dispose();
			}
		});
	}
	
	private WindowAdapter windowListener = new WindowAdapter() {
		
		@Override
		public void windowOpened(WindowEvent e) {
			timer.start();
		}
		

		@Override
		public void windowClosing(WindowEvent e) {
			if (timer != null)
				timer.stop();
		}
	};
	
	private MouseAdapter clickListener = new MouseAdapter() {
		
		public void mouseClicked(MouseEvent e) {
			close();
		}
	};
	
}
