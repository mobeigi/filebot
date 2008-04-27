/*
 * Created on 19.03.2005
 *
 */

package net.sourceforge.tuned.ui.notification;


import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;


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
		
		notification.addComponentListener(new RemoveListener(layout));
		layout.add(notification);
		notification.setVisible(true);
	}
	
	
	private static class RemoveListener extends ComponentAdapter {
		
		private NotificationLayout layout;
		
		
		public RemoveListener(NotificationLayout layout) {
			this.layout = layout;
		}
		

		@Override
		public void componentHidden(ComponentEvent e) {
			NotificationWindow window = (NotificationWindow) e.getSource();
			layout.remove(window);
			window.dispose();
		}
	}
	
}
