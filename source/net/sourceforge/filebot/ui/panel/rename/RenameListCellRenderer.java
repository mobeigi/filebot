
package net.sourceforge.filebot.ui.panel.rename;


import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.io.File;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import net.miginfocom.swing.MigLayout;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.ui.panel.rename.RenameModel.FormattedFuture;
import net.sourceforge.tuned.FileUtilities;
import net.sourceforge.tuned.ui.DefaultFancyListCellRenderer;
import net.sourceforge.tuned.ui.GradientStyle;


class RenameListCellRenderer extends DefaultFancyListCellRenderer {
	
	private final RenameModel renameModel;
	
	private final TypeRenderer typeRenderer = new TypeRenderer();
	
	private final Color noMatchGradientBeginColor = new Color(0xB7B7B7);
	private final Color noMatchGradientEndColor = new Color(0x9A9A9A);
	

	public RenameListCellRenderer(RenameModel renameModel) {
		super(new Insets(4, 7, 4, 7));
		
		this.renameModel = renameModel;
		
		setHighlightingEnabled(false);
		
		setLayout(new MigLayout("insets 0, fill", "align left", "align center"));
		add(typeRenderer, "gap rel:push, hidemode 3");
	}
	

	@Override
	public void configureListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		super.configureListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		
		// reset decoration
		setIcon(null);
		typeRenderer.setVisible(false);
		typeRenderer.setAlpha(1.0f);
		
		// render unmatched values differently
		if (!renameModel.hasComplement(index)) {
			if (isSelected) {
				setGradientColors(noMatchGradientBeginColor, noMatchGradientEndColor);
			} else {
				setForeground(noMatchGradientBeginColor);
				typeRenderer.setAlpha(0.5f);
			}
		}
		
		if (value instanceof File) {
			// display file extension
			File file = (File) value;
			
			if (renameModel.preserveExtension()) {
				setText(FileUtilities.getName(file));
				typeRenderer.setText(getType(file));
				typeRenderer.setVisible(true);
			} else {
				setText(file.getName());
			}
		} else if (value instanceof FormattedFuture) {
			// display progress icon
			FormattedFuture formattedFuture = (FormattedFuture) value;
			
			switch (formattedFuture.getState()) {
				case PENDING:
					setIcon(ResourceManager.getIcon("worker.pending"));
					break;
				case STARTED:
					setIcon(ResourceManager.getIcon("worker.started"));
					break;
			}
		}
	}
	

	protected String getType(File file) {
		if (file.isDirectory())
			return "Folder";
		
		String extension = FileUtilities.getExtension(file);
		
		if (extension != null)
			return extension.toLowerCase();
		
		// some file with no extension
		return "File";
	}
	

	private static class TypeRenderer extends DefaultListCellRenderer {
		
		private final Insets margin = new Insets(0, 10, 0, 0);
		private final Insets padding = new Insets(0, 6, 0, 5);
		private final int arc = 10;
		
		private Color gradientBeginColor = new Color(0xFFCC00);
		private Color gradientEndColor = new Color(0xFF9900);
		
		private float alpha = 1.0f;
		

		public TypeRenderer() {
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
			
			g2d.setPaint(GradientStyle.TOP_TO_BOTTOM.getGradientPaint(shape, gradientBeginColor, gradientEndColor));
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
