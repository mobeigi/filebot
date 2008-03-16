package net.sourceforge.filebot.ui.panel.rename;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.geom.Rectangle2D;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter;

import net.sourceforge.tuned.ui.GradientStyle;


public class HighlightListCellRenderer extends JTextField implements ListCellRenderer {
	
	private Pattern pattern;
	private Highlighter.HighlightPainter highlightPainter;
	
	private Color gradientBeginColor;
	private Color gradientEndColor;
	private GradientStyle gradientStyle;
	private Insets margin;
	private boolean highlightingEnabled;
	private boolean selected;
	private Border border;
	
	
	public HighlightListCellRenderer(Pattern pattern, Highlighter.HighlightPainter highlightPainter, int padding, boolean highlighting) {
		this(pattern, highlightPainter, GradientStyle.TOP_TO_BOTTOM, highlighting, new Insets(padding, padding, padding, padding), new Insets(1, 1, 0, 1), null);
	}
	

	public HighlightListCellRenderer(Pattern pattern, Highlighter.HighlightPainter highlightPainter, GradientStyle gradientStyle, boolean highlighting, Insets padding, Insets margin, Color selectedBorderColor) {
		this.pattern = pattern;
		this.highlightPainter = highlightPainter;
		
		this.gradientStyle = gradientStyle;
		this.margin = margin;
		this.highlightingEnabled = highlighting;
		
		border = new EmptyBorder(padding);
		border = new CompoundBorder(new LineBorder(selectedBorderColor, 1), border);
		border = new CompoundBorder(new EmptyBorder(margin), border);
		
		setOpaque(false);
		
		this.getDocument().addDocumentListener(new HighlightUpdateListener());
	}
	

	@Override
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		if (isSelected) {
			setBackground(list.getSelectionBackground());
			setForeground(list.getSelectionForeground());
		} else {
			setBackground(list.getBackground());
			setForeground(list.getForeground());
		}
		
		setText(value.toString());
		
		setSelected(isSelected);
		setBorder(border);
		
		Color sc = list.getSelectionBackground();
		
		if (highlightingEnabled) {
			Color normalBg = list.getBackground();
			Color highlightBg = new Color(sc.getRed(), sc.getGreen(), sc.getBlue(), 8);
			
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
	

	protected void updateHighlighter() {
		getHighlighter().removeAllHighlights();
		
		Matcher matcher = pattern.matcher(getText());
		
		while (matcher.find()) {
			try {
				getHighlighter().addHighlight(matcher.start(0), matcher.end(0), highlightPainter);
			} catch (BadLocationException e) {
				//should not happen
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, e.toString(), e);
			}
		}
	}
	

	public void setGradientBeginColor(Color gradientBeginColor) {
		this.gradientBeginColor = gradientBeginColor;
	}
	

	public void setGradientEndColor(Color gradientEndColor) {
		this.gradientEndColor = gradientEndColor;
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
	
	
	private class HighlightUpdateListener implements DocumentListener {
		
		@Override
		public void changedUpdate(DocumentEvent e) {
			
		}
		

		@Override
		public void insertUpdate(DocumentEvent e) {
			updateHighlighter();
		}
		

		@Override
		public void removeUpdate(DocumentEvent e) {
			updateHighlighter();
		}
		
	}
	
}
