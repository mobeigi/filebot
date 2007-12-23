/*
 * Created on 19.03.2005
 *
 */

package net.sourceforge.tuned.ui.notification;


import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;


/**
 * @author Reinhard
 * 
 */
public class NotificationManager {
	
	private NotificationLayout layout;
	
	
	public NotificationManager() {
		this(new QueueNotificationLayout());
	}
	

	public NotificationManager(NotificationLayout layout) {
		setLayoutManager(layout);
	}
	

	public void setLayoutManager(NotificationLayout layout) {
		this.layout = layout;
	}
	

	public NotificationLayout getLayoutManager() {
		return layout;
	}
	

	public void show(NotificationWindow notification) {
		if (layout == null)
			return;
		
		notification.addWindowListener(new RemoveListener(layout));
		layout.add(notification);
		notification.setVisible(true);
	}
	
	
	private static class RemoveListener extends WindowAdapter {
		
		private NotificationLayout layout;
		
		
		public RemoveListener(NotificationLayout layout) {
			this.layout = layout;
		}
		

		public void windowClosing(WindowEvent e) {
			NotificationWindow n = (NotificationWindow) e.getSource();
			layout.remove(n);
			n.dispose();
		}
	}
	
}
