/*
 * Created on 20.03.2005
 */

package net.sourceforge.tuned.ui.notification;


import static net.sourceforge.tuned.ui.notification.Direction.*;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;


public class SimpleNotificationLayout implements NotificationLayout {
	
	private NotificationWindow currentNotification;
	private Direction alignment;
	
	
	public SimpleNotificationLayout() {
		this(NORTH);
	}
	

	public SimpleNotificationLayout(Direction alignment) {
		this.alignment = alignment;
	}
	

	private Point getBaseAnchor(Dimension screen, Insets insets) {
		Point p = new Point();
		
		screen.height -= insets.top + insets.bottom;
		screen.width -= insets.left + insets.right;
		
		p.x = (int) (alignment.ax * screen.width);
		p.y = (int) (alignment.ay * screen.height);
		
		p.x += insets.left;
		p.y += insets.top;
		
		return p;
	}
	

	private Point getLocation(Point anchor, Dimension size) {
		Point p = new Point();
		
		p.x = (int) (anchor.x - size.width * alignment.ax);
		p.y = (int) (anchor.y - size.height * alignment.ay);
		
		return p;
	}
	

	public void add(NotificationWindow notification) {
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(notification.getGraphicsConfiguration());
		Dimension size = notification.getSize();
		
		Point anchor = getBaseAnchor(screen, insets);
		notification.setLocation(getLocation(anchor, size));
		
		if (currentNotification != null) {
			currentNotification.close();
		}
		
		currentNotification = notification;
	}
	

	public void remove(NotificationWindow notification) {
		
	}
	
}
