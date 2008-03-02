
package net.sourceforge.tuned.ui;


import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
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
	private GradientStyle gradientStyle;
	private Insets margin;
	private boolean highlightingEnabled;
	private boolean selected;
	private Border border;
	
	
	public FancyListCellRenderer() {
		this(GradientStyle.TOP_TO_BOTTOM, true, new Insets(7, 7, 7, 7), new Insets(1, 1, 0, 1), null);
	}
	

	public FancyListCellRenderer(int padding, Color selectedBorderColor, GradientStyle gradientStyle) {
		this(gradientStyle, false, new Insets(padding, padding, padding, padding), new Insets(0, 0, 0, 0), selectedBorderColor);
	}
	

	public FancyListCellRenderer(GradientStyle gradientStyle, boolean highlighting, Insets padding, Insets margin, Color selectedBorderColor) {
		this.gradientStyle = gradientStyle;
		this.margin = margin;
		this.highlightingEnabled = highlighting;
		
		border = new EmptyBorder(padding);
		border = new CompoundBorder(new LineBorder(selectedBorderColor, 1), border);
		border = new CompoundBorder(new EmptyBorder(margin), border);
		
		setOpaque(false);
	}
	

	@Override
	protected void paintBorder(Graphics g) {
		if (selected) {
			super.paintBorder(g);
		}
	}
	

	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		Rectangle2D shape = new Rectangle2D.Double(margin.left, margin.top, getWidth() - (margin.left + margin.right), getHeight() - (margin.top + margin.bottom));
		
		if (highlightingEnabled) {
			g2d.setPaint(getBackground());
			g2d.fill(shape);
		}
		
		if (selected) {
			g2d.setPaint(gradientStyle.getGradientPaint(shape, gradientBeginColor, gradientEndColor));
			g2d.fill(shape);
		}
		
		super.paintComponent(g);
	}
	

	@Override
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		
		setSelected(isSelected);
		setBorder(border);
		
		Color sc = list.getSelectionBackground();
		
		if (highlightingEnabled) {
			Color normalBg = list.getBackground();
			Color highlightBg = new Color(sc.getRed(), sc.getGreen(), sc.getBlue(), 28);
			
			if ((index % 2) == 0)
				setBackground(highlightBg);
			else
				setBackground(normalBg);
		}
		
		if (isSelected) {
			setGradientBeginColor(sc.brighter());
			setGradientEndColor(sc);
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
	

	public boolean isHighlightingEnabled() {
		return highlightingEnabled;
	}
	

	public void setHighlightingEnabled(boolean highlightingEnabled) {
		this.highlightingEnabled = highlightingEnabled;
	}
	

	public void setSelected(boolean selected) {
		this.selected = selected;
	}
	

	public boolean isSelected() {
		return selected;
	}
}
