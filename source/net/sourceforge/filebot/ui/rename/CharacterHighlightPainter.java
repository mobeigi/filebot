
package net.sourceforge.filebot.ui.rename;


import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.plaf.TextUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;

import net.sourceforge.tuned.ui.GradientStyle;


class CharacterHighlightPainter implements Highlighter.HighlightPainter {
	
	private Color gradientBeginColor;
	private Color gradientEndColor;
	
	
	public CharacterHighlightPainter(Color gradientBeginColor, Color gradientEndColor) {
		this.gradientBeginColor = gradientBeginColor;
		this.gradientEndColor = gradientEndColor;
	}
	

	@Override
	public void paint(Graphics g, int offset1, int offset2, Shape bounds, JTextComponent c) {
		Graphics2D g2d = (Graphics2D) g;
		
		try {
			// determine locations
			TextUI mapper = c.getUI();
			Rectangle p1 = mapper.modelToView(c, offset1);
			Rectangle p2 = mapper.modelToView(c, offset2);
			
			Rectangle r = p1.union(p2);
			
			float w = r.width + 1;
			float h = r.height;
			
			float x = r.x - 1;
			float y = r.y;
			
			float arch = 5f;
			
			RoundRectangle2D shape = new RoundRectangle2D.Float(x, y, w, h, arch, arch);
			
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2d.setPaint(GradientStyle.TOP_TO_BOTTOM.getGradientPaint(shape, gradientBeginColor, gradientEndColor));
			
			g2d.fill(shape);
		} catch (BadLocationException e) {
			//should not happen
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, e.toString(), e);
		}
	}
}
