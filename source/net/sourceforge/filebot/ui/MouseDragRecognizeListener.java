
package net.sourceforge.filebot.ui;


import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.TransferHandler;


public class MouseDragRecognizeListener extends MouseAdapter {
	
	private MouseEvent firstMouseEvent = null;
	
	private int dragShift = 5;
	
	
	@Override
	public void mousePressed(MouseEvent e) {
		firstMouseEvent = e;
		e.consume();
	}
	

	@Override
	public void mouseDragged(MouseEvent e) {
		if (firstMouseEvent != null) {
			e.consume();
			int dx = Math.abs(e.getX() - firstMouseEvent.getX());
			int dy = Math.abs(e.getY() - firstMouseEvent.getY());
			
			if (dx > dragShift || dy > dragShift) {
				// This is a drag, not a click.
				JComponent c = (JComponent) e.getSource();
				TransferHandler handler = c.getTransferHandler();
				handler.exportAsDrag(c, firstMouseEvent, TransferHandler.COPY);
				firstMouseEvent = null;
			}
		}
	}
	

	@Override
	public void mouseReleased(MouseEvent e) {
		firstMouseEvent = null;
	}
	

	public static void createForComponent(JComponent component) {
		MouseDragRecognizeListener l = new MouseDragRecognizeListener();
		
		component.addMouseListener(l);
		component.addMouseMotionListener(l);
	}
	
}
