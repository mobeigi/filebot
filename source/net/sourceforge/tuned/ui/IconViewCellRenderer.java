
package net.sourceforge.tuned.ui;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.RectangularShape;
import java.awt.geom.RoundRectangle2D;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;


public class IconViewCellRenderer extends AbstractFancyListCellRenderer {
	
	private final JLabel iconLabel = new JLabel();
	private final JLabel titleLabel = new JLabel();
	
	private final ContentPane contentPane = new ContentPane();
	
	
	public IconViewCellRenderer() {
		super(new Insets(3, 3, 3, 3), new Insets(3, 3, 3, 3));
		
		setHighlightingEnabled(false);
		
		contentPane.add(titleLabel);
		contentPane.setBorder(new EmptyBorder(4, 4, 4, 4));
		
		iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
		iconLabel.setVerticalAlignment(SwingConstants.CENTER);
		
		JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.setOpaque(false);
		
		Box contentPaneContainer = new Box(BoxLayout.X_AXIS);
		contentPaneContainer.add(contentPane);
		
		contentPanel.add(contentPaneContainer, BorderLayout.WEST);
		
		add(iconLabel, BorderLayout.WEST);
		add(contentPanel, BorderLayout.CENTER);
	}
	

	@Override
	protected void configureListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		super.configureListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		
		setGradientPainted(false);
		
		setText(value.toString());
		contentPane.setGradientPainted(isSelected);
	}
	

	@Override
	public void setForeground(Color fg) {
		super.setForeground(fg);
		
		// label is null while in super constructor
		if (titleLabel != null) {
			titleLabel.setForeground(fg);
		}
	}
	

	@Override
	public void setBackground(Color bg) {
		super.setBackground(bg);
		
		// label is null while in super constructor
		if (titleLabel != null) {
			titleLabel.setBackground(bg);
		}
	}
	

	public void setIcon(Icon icon) {
		iconLabel.setIcon(icon);
	}
	

	public void setText(String title) {
		titleLabel.setText(title);
	}
	

	protected JComponent getContentPane() {
		return contentPane;
	}
	
	
	private class ContentPane extends Box {
		
		private boolean gradientPainted;
		
		
		public ContentPane() {
			super(BoxLayout.Y_AXIS);
			setOpaque(false);
		}
		

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2d = (Graphics2D) g;
			
			RectangularShape shape = new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16);
			
			if (gradientPainted) {
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2d.setPaint(getGradientStyle().getGradientPaint(shape, getGradientBeginColor(), getGradientEndColor()));
				g2d.fill(shape);
			}
			
			super.paintComponent(g);
		}
		

		public void setGradientPainted(boolean gradientPainted) {
			this.gradientPainted = gradientPainted;
		}
		
	}
	
	
	/**
	 * Overridden for performance reasons.
	 */
	@Override
	public void validate() {
		// validate children, yet avoid flickering of the mouse cursor
		validateTree();
	}
	

	/**
	 * Overridden for performance reasons.
	 */
	@Override
	public void repaint() {
	}
	

	/**
	 * Overridden for performance reasons.
	 */
	@Override
	public void repaint(long tm, int x, int y, int width, int height) {
	}
	

	/**
	 * Overridden for performance reasons.
	 */
	@Override
	public void repaint(Rectangle r) {
	}
	

	/**
	 * Overridden for performance reasons.
	 */
	@Override
	protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
	}
	

	/**
	 * Overridden for performance reasons.
	 */
	@Override
	public void firePropertyChange(String propertyName, byte oldValue, byte newValue) {
	}
	

	/**
	 * Overridden for performance reasons.
	 */
	@Override
	public void firePropertyChange(String propertyName, char oldValue, char newValue) {
	}
	

	/**
	 * Overridden for performance reasons.
	 */
	@Override
	public void firePropertyChange(String propertyName, short oldValue, short newValue) {
	}
	

	/**
	 * Overridden for performance reasons.
	 */
	@Override
	public void firePropertyChange(String propertyName, int oldValue, int newValue) {
	}
	

	/**
	 * Overridden for performance reasons.
	 */
	@Override
	public void firePropertyChange(String propertyName, long oldValue, long newValue) {
	}
	

	/**
	 * Overridden for performance reasons.
	 */
	@Override
	public void firePropertyChange(String propertyName, float oldValue, float newValue) {
	}
	

	/**
	 * Overridden for performance reasons.
	 */
	@Override
	public void firePropertyChange(String propertyName, double oldValue, double newValue) {
	}
	

	/**
	 * Overridden for performance reasons.
	 */
	@Override
	public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
	}
	
}
