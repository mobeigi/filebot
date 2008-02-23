
package net.sourceforge.filebot.ui.panel.search;


import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.SystemColor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JLabel;


public class HyperlinkLabel extends JLabel {
	
	private URL url;
	private Color defaultColor;
	
	
	public HyperlinkLabel(String label, URL url) {
		super(label);
		this.url = url;
		defaultColor = getForeground();
		addMouseListener(linker);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	}
	
	private MouseListener linker = new MouseAdapter() {
		
		@Override
		public void mouseClicked(MouseEvent event) {
			try {
				Desktop.getDesktop().browse(url.toURI());
			} catch (Exception e) {
				// should not happen
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, e.getMessage(), e);
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
