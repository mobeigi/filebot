
package net.sourceforge.tuned.ui;


import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;


public class FancyListCellRenderer extends DefaultListCellRenderer {
	
	private Color gradientBeginColor;
	
	private Color gradientEndColor;
	
	private Border defaultBorder;
	
	private Border selectedBorder;
	
	private GradientStyle gradientStyle;
	
	private boolean paintGradient;
	
	
	public FancyListCellRenderer() {
		this(7, GradientStyle.TOP_TO_BOTTOM);
	}
	

	public FancyListCellRenderer(int margin, GradientStyle gradientStyle) {
		this(margin, null, gradientStyle);
	}
	

	public FancyListCellRenderer(int margin, Color selectedBorderColor, GradientStyle gradientStyle) {
		this.gradientStyle = gradientStyle;
		
		Border marginBorder = new EmptyBorder(margin, margin, margin, margin);
		
		if (selectedBorderColor != null) {
			defaultBorder = new CompoundBorder(new EmptyBorder(1, 1, 1, 1), marginBorder);
			selectedBorder = new CompoundBorder(new LineBorder(selectedBorderColor, 1), marginBorder);
		} else {
			defaultBorder = marginBorder;
			selectedBorder = marginBorder;
		}
		
	}
	

	@Override
	protected void paintComponent(Graphics g) {
		if (isPaintGradient()) {
			Graphics2D g2d = (Graphics2D) g;
			
			Rectangle2D shape = new Rectangle2D.Double(0, 0, getWidth(), getHeight());
			
			GradientPaint gradient = gradientStyle.getGradientPaint(shape, gradientBeginColor, gradientEndColor);
			g2d.setPaint(gradient);
			g2d.fill(shape);
		}
		
		super.paintComponent(g);
	}
	

	@Override
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		
		setOpaque(false);
		
		if (isSelected) {
			setPaintGradient(true);
			
			Color c = list.getSelectionBackground();
			setGradientBeginColor(c.brighter());
			setGradientEndColor(c);
			
			setBorder(selectedBorder);
		} else {
			setPaintGradient(false);
			setBorder(defaultBorder);
		}
		
		return this;
	}
	

	public Color getGradientBeginColor() {
		return gradientBeginColor;
	}
	

	public void setGradientBeginColor(Color gradientBeginColor) {
		this.gradientBeginColor = gradientBeginColor;
	}
	

	public Color getGradientEndColor() {
		return gradientEndColor;
	}
	

	public void setGradientEndColor(Color gradientEndColor) {
		this.gradientEndColor = gradientEndColor;
	}
	

	public GradientStyle getGradientStyle() {
		return gradientStyle;
	}
	

	public void setGradientStyle(GradientStyle gradientStyle) {
		this.gradientStyle = gradientStyle;
	}
	

	public boolean isPaintGradient() {
		return paintGradient;
	}
	

	public void setPaintGradient(boolean gradientEnabled) {
		this.paintGradient = gradientEnabled;
	}
	
}
