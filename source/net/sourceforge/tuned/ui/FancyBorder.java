
package net.sourceforge.tuned.ui;


import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;
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
	
	
	public FancyBorder(int width, Color color) {
		this.borderWidth = width;
		
		float[] dist = { 0, 1 };
		this.dist = dist;
		
		Color[] colors = { color.brighter(), color };
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
		return new Insets(borderWidth, borderWidth, borderWidth, borderWidth);
	}
	

	@Override
	public boolean isBorderOpaque() {
		return false;
	}
	

	@Override
	public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
		Graphics2D g2d = (Graphics2D) g;
		
		Shape shape = new RoundRectangle2D.Double(x, y, width, height, 10, 10);
		
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setPaint(getPaint(x, y, width, height));
		g2d.setStroke(new BasicStroke(borderWidth));
		
		g2d.draw(shape);
	}
	

	private Paint getPaint(int x, int y, int width, int height) {
		Point2D center = new Point2D.Float(width, 0);
		
		return new RadialGradientPaint(center, radius, dist, colors, CycleMethod.REFLECT);
	}
}
