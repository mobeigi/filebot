
package net.sourceforge.tuned.ui;


import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.SystemColor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JLabel;


public class HyperlinkLabel extends JLabel {
	
	private final URI link;
	private final Color defaultColor;
	
	
	public HyperlinkLabel(String label, URI link) {
		super(label);
		this.link = link;
		defaultColor = getForeground();
		addMouseListener(linker);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	}
	
	private MouseListener linker = new MouseAdapter() {
		
		@Override
		public void mouseClicked(MouseEvent event) {
			try {
				Desktop.getDesktop().browse(link);
			} catch (Exception e) {
				// should not happen
				Logger.getLogger("global").log(Level.SEVERE, e.toString(), e);
			}
		}
		

		@Override
		public void mouseEntered(MouseEvent e) {
			setForeground(SystemColor.textHighlight);
		}
		

		@Override
		public void mouseExited(MouseEvent e) {
			setForeground(defaultColor);
		}
	};
	
}
