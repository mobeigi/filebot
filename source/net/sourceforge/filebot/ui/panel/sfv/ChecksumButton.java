
package net.sourceforge.filebot.ui.panel.sfv;


import static java.awt.Color.WHITE;
import static java.awt.Cursor.DEFAULT_CURSOR;
import static java.awt.Cursor.HAND_CURSOR;
import static java.awt.Cursor.getPredefinedCursor;
import static java.awt.Font.DIALOG;
import static java.awt.Font.PLAIN;
import static java.awt.RenderingHints.KEY_RENDERING;
import static java.awt.RenderingHints.KEY_TEXT_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_RENDER_QUALITY;
import static java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON;
import static java.lang.Math.max;
import static java.lang.Math.round;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JToggleButton;

import net.sourceforge.filebot.ResourceManager;


public class ChecksumButton extends JToggleButton {
	
	private static final Icon contentArea = ResourceManager.getIcon("button.checksum");
	private static final Icon contentAreaSelected = ResourceManager.getIcon("button.checksum.selected");
	
	
	public ChecksumButton(Action action) {
		super(action);
		
		setPreferredSize(new Dimension(max(contentAreaSelected.getIconWidth(), contentArea.getIconWidth()), max(contentAreaSelected.getIconHeight(), contentArea.getIconHeight())));
		setMinimumSize(getPreferredSize());
		setMaximumSize(getPreferredSize());
		
		setForeground(WHITE);
		setFont(new Font(DIALOG, PLAIN, 11));
		
		setContentAreaFilled(false);
		setFocusPainted(false);
		
		setEnabled(true);
	}
	

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		
		// set appropriate cursor
		setCursor(getPredefinedCursor(enabled ? HAND_CURSOR : DEFAULT_CURSOR));
	}
	

	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		
		g2d.setRenderingHint(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_ON);
		g2d.setRenderingHint(KEY_RENDERING, VALUE_RENDER_QUALITY);
		
		// paint background image in the center
		if (isSelected()) {
			contentAreaSelected.paintIcon(this, g2d, (int) round((getWidth() - contentAreaSelected.getIconWidth()) / (double) 2), (int) round((getHeight() - contentAreaSelected.getIconHeight()) / (double) 2));
		} else {
			contentArea.paintIcon(this, g2d, (int) round((getWidth() - contentArea.getIconWidth()) / (double) 2), (int) round((getHeight() - contentArea.getIconHeight()) / (double) 2));
		}
		
		Rectangle2D textBounds = g2d.getFontMetrics().getStringBounds(getText(), g2d);
		
		// draw text in the center
		g2d.drawString(getText(), round((getWidth() - textBounds.getWidth()) / 2) + 1, round(getHeight() / 2 - textBounds.getY() - textBounds.getHeight() / 2));
	}
}
