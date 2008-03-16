
package net.sourceforge.tuned.ui;


import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.border.Border;


public class FancyBorder implements Border {
	
	private int borderWidth;
	
	private float[] dist;
	private Color[] colors;
	
	private float radius;
	
	
	public FancyBorder(int width, Color... colors) {
		this.borderWidth = width;
		
		this.dist = new float[colors.length];
		
		for (int i = 0; i < colors.length; i++) {
			this.dist[i] = (1.0f / colors.length) * i;
		}
		
		this.colors = colors;
		
		this.radius = 100;
	}
	

	public FancyBorder(int width, float[] dist, Color[] colors, float radius) {
		this.borderWidth = width;
		this.dist = dist;
		this.colors = colors;
		this.radius = radius;
	}
	

	@Override
	public Insets getBorderInsets(Component c) {
		
		int horizontalOffset = 8;
		return new Insets(borderWidth, borderWidth + horizontalOffset, borderWidth, borderWidth + horizontalOffset);
	}
	

	@Override
	public boolean isBorderOpaque() {
		return false;
	}
	

	@Override
	public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
		Graphics2D g2d = (Graphics2D) g;
		
		float arch = Math.min(width, height) / 2;
		
		Shape shape = new RoundRectangle2D.Float(x + borderWidth, y + borderWidth, width - borderWidth * 2, height - borderWidth * 2, arch, arch);
		
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		Point2D center = new Point2D.Float(width, 0);
		g2d.setPaint(new RadialGradientPaint(center, radius, dist, colors, CycleMethod.REFLECT));
		
		g2d.setStroke(new BasicStroke(borderWidth));
		
		g2d.draw(shape);
	}
}
