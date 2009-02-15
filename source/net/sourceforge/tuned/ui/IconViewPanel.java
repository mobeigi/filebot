
package net.sourceforge.tuned.ui;


import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.geom.Rectangle2D;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;


public class IconViewPanel extends JPanel {
	
	private final JList list = new JList(createModel());
	
	private final JLabel titleLabel = new JLabel();
	
	private final JPanel headerPanel = new JPanel(new BorderLayout());
	
	
	public IconViewPanel() {
		super(new BorderLayout());
		
		list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
		list.setVisibleRowCount(-1);
		
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
		
		headerPanel.setOpaque(false);
		headerPanel.add(titleLabel, BorderLayout.WEST);
		
		setBackground(list.getBackground());
		
		headerPanel.setBorder(new CompoundBorder(new GradientLineBorder(290), new EmptyBorder(2, 10, 1, 5)));
		list.setBorder(new EmptyBorder(5, 5, 5, 5));
		
		JScrollPane listScrollPane = new JScrollPane(list);
		listScrollPane.setBorder(null);
		
		add(headerPanel, BorderLayout.NORTH);
		add(listScrollPane, BorderLayout.CENTER);
	}
	

	protected ListModel createModel() {
		return new DefaultListModel();
	}
	

	public JList getList() {
		return list;
	}
	

	public JPanel getHeaderPanel() {
		return headerPanel;
	}
	

	public void setTitle(String text) {
		titleLabel.setText(text);
	}
	

	public String getTitle() {
		return titleLabel.getText();
	}
	

	public void expand() {
		// TODO expand
	}
	

	public void collapse() {
		// TODO collapse
	}
	
	
	private class GradientLineBorder implements Border {
		
		private int gradientLineWidth;
		
		
		public GradientLineBorder(int width) {
			this.gradientLineWidth = width;
		}
		

		@Override
		public Insets getBorderInsets(Component c) {
			return new Insets(0, 0, 1, 0);
		}
		

		@Override
		public boolean isBorderOpaque() {
			return false;
		}
		

		@Override
		public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
			Graphics2D g2d = (Graphics2D) g;
			
			Color beginColor = list.getSelectionBackground().brighter();
			Color endColor = list.getBackground();
			
			Rectangle2D shape = new Rectangle2D.Float(x, y + height - 1, gradientLineWidth, 1);
			
			Paint paint = GradientStyle.LEFT_TO_RIGHT.getGradientPaint(shape, beginColor, endColor);
			g2d.setPaint(paint);
			
			g2d.setComposite(AlphaComposite.SrcOver.derive(0.9f));
			
			g2d.fill(shape);
		}
	}
	
}
