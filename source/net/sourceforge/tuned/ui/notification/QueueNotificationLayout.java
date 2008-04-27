/*
 * Created on 19.03.2005
 *
 */

package net.sourceforge.tuned.ui.notification;


import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.ListIterator;

import javax.swing.SwingConstants;


public class QueueNotificationLayout implements NotificationLayout, SwingConstants {
	
	private ArrayList<NotificationWindow> notificationList = new ArrayList<NotificationWindow>();
	
	private int orientation;
	private int direction;
	private int growAnchor;
	
	
	public QueueNotificationLayout() {
		this(SOUTH_EAST, WEST);
	}
	

	public QueueNotificationLayout(int orientation, int direction) {
		this.orientation = orientation;
		this.growAnchor = orientation;
		this.direction = direction;
	}
	

	public QueueNotificationLayout(int orientation, int direction, int growAnchor) {
		this.orientation = orientation;
		this.direction = direction;
		this.growAnchor = growAnchor;
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
		Factor f = Factor.getOrientationFactor(growAnchor);
		
		Point p = new Point();
		p.x = (int) (anchor.x - size.width * f.fx);
		p.y = (int) (anchor.y - size.height * f.fy);
		
		return p;
	}
	

	private Point getNextAnchor(Point anchor, Dimension size) {
		Factor f = Factor.getDirectionFactor(direction);
		
		Point p = new Point();
		p.x = (int) (anchor.x + size.width * f.fx);
		p.y = (int) (anchor.y + size.height * f.fy);
		return p;
	}
	

	public void add(NotificationWindow notification) {
		notificationList.add(notification);
		align(notification.getGraphicsConfiguration());
	}
	

	private void align(GraphicsConfiguration gc) {
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
		
		Point anchor = getBaseAnchor(screen, insets);
		
		ListIterator<NotificationWindow> i = notificationList.listIterator();
		
		while (i.hasNext()) {
			NotificationWindow n = i.next();
			Dimension size = n.getSize();
			
			Point p = getLocation(anchor, size);
			n.setLocation(p);
			
			anchor = getNextAnchor(anchor, size);
		}
	}
	

	public void remove(NotificationWindow notification) {
		if (notificationList.remove(notification)) {
			align(notification.getGraphicsConfiguration());
		}
	}
	
}
