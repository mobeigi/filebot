/*
 * Created on 20.03.2005
 *
 */

package net.sourceforge.tuned.ui.notification;


import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;

import javax.swing.SwingConstants;


/**
 * @author Reinhard
 * 
 */
public class SimpleNotificationLayout implements NotificationLayout, SwingConstants {
	
	private NotificationWindow currentNotification;
	private int orientation;
	
	
	public SimpleNotificationLayout() {
		this(NORTH);
	}
	

	public SimpleNotificationLayout(int orientation) {
		this.orientation = orientation;
	}
	

	private Point getBaseAnchor(Dimension screen, Insets insets) {
		Factor f = Factor.getOrientationFactor(orientation);
		
		Point p = new Point();
		
		screen.height -= insets.top + insets.bottom;
		screen.width -= insets.left + insets.right;
		
		p.x = (int) (f.fx * screen.width);
		p.y = (int) (f.fy * screen.height);
		
		p.x += insets.left;
		p.y += insets.top;
		
		return p;
	}
	

	private Point getLocation(Point anchor, Dimension size) {
		Factor f = Factor.getOrientationFactor(orientation);
		
		Point p = new Point();
		p.x = (int) (anchor.x - size.width * f.fx);
		p.y = (int) (anchor.y - size.height * f.fy);
		
		return p;
	}
	

	public void add(NotificationWindow notification) {
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(notification.getGraphicsConfiguration());
		Dimension size = notification.getSize();
		
		Point anchor = getBaseAnchor(screen, insets);
		notification.setLocation(getLocation(anchor, size));
		
		if (currentNotification != null)
			currentNotification.close();
		
		currentNotification = notification;
	}
	

	public void remove(NotificationWindow notification) {
		
	}
	
}
