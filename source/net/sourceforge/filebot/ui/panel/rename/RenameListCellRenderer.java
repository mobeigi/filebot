
package net.sourceforge.filebot.ui.panel.rename;


import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import net.sourceforge.tuned.ui.DefaultFancyListCellRenderer;


class RenameListCellRenderer extends DefaultFancyListCellRenderer {
	
	private final RenameModel model;
	
	private final ExtensionLabel extension = new ExtensionLabel();
	
	
	public RenameListCellRenderer(RenameModel model) {
		this.model = model;
		
		setHighlightingEnabled(false);
		
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		add(Box.createHorizontalGlue());
		add(extension);
	}
	
	private final Color noMatchGradientBeginColor = new Color(0xB7B7B7);
	private final Color noMatchGradientEndColor = new Color(0x9A9A9A);
	
	
	@Override
	public void configureListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		super.configureListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		
		// show extension label only for items of the files model
		if (value instanceof FileEntry) {
			FileEntry entry = (FileEntry) value;
			
			extension.setText(entry.getType());
			extension.setVisible(true);
		} else {
			extension.setVisible(false);
		}
		
		extension.setAlpha(1.0f);
		
		if (index >= model.matchCount()) {
			if (isSelected) {
				setGradientColors(noMatchGradientBeginColor, noMatchGradientEndColor);
			} else {
				setForeground(noMatchGradientBeginColor);
				extension.setAlpha(0.5f);
			}
		}
	}
	
	
	protected class ExtensionLabel extends JLabel {
		
		private final Insets margin = new Insets(0, 10, 0, 0);
		private final Insets padding = new Insets(0, 6, 0, 5);
		private final int arc = 10;
		
		private Color gradientBeginColor = new Color(0xFFCC00);
		private Color gradientEndColor = new Color(0xFF9900);
		
		private float alpha = 1.0f;
		
		
		public ExtensionLabel() {
			setOpaque(false);
			setForeground(new Color(0x141414));
			
			setBorder(new CompoundBorder(new EmptyBorder(margin), new EmptyBorder(padding)));
		}
		

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2d = (Graphics2D) g;
			
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			
			RoundRectangle2D shape = new RoundRectangle2D.Float(margin.left, margin.top, getWidth() - (margin.left + margin.right), getHeight(), arc, arc);
			
			g2d.setComposite(AlphaComposite.SrcOver.derive(alpha));
			
			g2d.setPaint(getGradientStyle().getGradientPaint(shape, gradientBeginColor, gradientEndColor));
			g2d.fill(shape);
			
			g2d.setFont(getFont());
			g2d.setPaint(getForeground());
			
			Rectangle2D textBounds = g2d.getFontMetrics().getStringBounds(getText(), g2d);
			g2d.drawString(getText(), (float) (shape.getCenterX() - textBounds.getX() - (textBounds.getWidth() / 2f)), (float) (shape.getCenterY() - textBounds.getY() - (textBounds.getHeight() / 2)));
		}
		

		public void setAlpha(float alpha) {
			this.alpha = alpha;
		}
	}
	
}
