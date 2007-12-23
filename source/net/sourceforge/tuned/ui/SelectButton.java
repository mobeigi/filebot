
package net.sourceforge.tuned.ui;


import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;


public class SelectButton<T> extends JButton implements ActionListener {
	
	private boolean hover = false;
	
	private Color beginColor = Color.decode("#F0EEE4");
	private Color endColor = Color.decode("#E0DED4");
	
	private Color beginColorHover = beginColor;
	private Color endColorHover = Color.decode("#D8D7CD");
	
	private T selectedValue = null;
	
	private List<T> options;
	private Map<T, ? extends Icon> icons;
	
	public static final String SELECTED_VALUE_PROPERTY = "SELECTED_VALUE_PROPERTY";
	
	
	public SelectButton(List<T> options, Map<T, ? extends Icon> icons) {
		this.options = options;
		this.icons = icons;
		
		setContentAreaFilled(false);
		setFocusable(false);
		
		addActionListener(this);
		
		setHorizontalAlignment(SwingConstants.CENTER);
		setVerticalAlignment(SwingConstants.CENTER);
		
		addMouseListener(new MouseInputListener());
		
		// select first option
		setSelectedValue(options.iterator().next());
		setPreferredSize(new Dimension(32, 22));
	}
	

	public void setSelectedValue(T value) {
		if (selectedValue == value)
			return;
		
		T oldSelectedValue = selectedValue;
		
		selectedValue = value;
		Icon icon = icons.get(selectedValue);
		setIcon(new SelectIcon(icon));
		
		firePropertyChange(SELECTED_VALUE_PROPERTY, oldSelectedValue, selectedValue);
	}
	

	public T getSelectedValue() {
		return selectedValue;
	}
	

	public void actionPerformed(ActionEvent e) {
		JPopupMenu popup = new JPopupMenu();
		
		for (T option : options) {
			popup.add(new SelectMenuIem(option));
		}
		
		popup.show(this, 0, getHeight() - 1);
	}
	

	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		
		if (hover)
			g2d.setPaint(GradientStyle.TOP_TO_BOTTOM.getGradientPaint(getBounds(), beginColorHover, endColorHover));
		else
			g2d.setPaint(GradientStyle.TOP_TO_BOTTOM.getGradientPaint(getBounds(), beginColor, endColor));
		
		g2d.fill(getBounds());
		
		super.paintComponent(g);
	}
	
	
	private class SelectMenuIem extends JMenuItem implements ActionListener {
		
		private T value;
		
		
		public SelectMenuIem(T value) {
			super(value.toString(), icons.get(value));
			this.value = value;
			this.setMargin(new Insets(3, 0, 3, 0));
			this.addActionListener(this);
			
			if (this.value == getSelectedValue())
				this.setFont(this.getFont().deriveFont(Font.BOLD));
		}
		

		public void actionPerformed(ActionEvent e) {
			setSelectedValue(value);
		}
		
	}
	

	private static class SelectIcon implements Icon {
		
		private Icon icon;
		private GeneralPath arrow;
		
		
		public SelectIcon(Icon icon) {
			this.icon = icon;
			
			arrow = new GeneralPath(Path2D.WIND_EVEN_ODD, 3);
			int x = 25;
			int y = 10;
			
			arrow.moveTo(x - 2, y);
			arrow.lineTo(x, y + 3);
			arrow.lineTo(x + 3, y);
			arrow.lineTo(x - 2, y);
		}
		

		public synchronized void paintIcon(Component c, Graphics g, int x, int y) {
			Graphics2D g2d = (Graphics2D) g;
			
			icon.paintIcon(c, g2d, 4, 3);
			
			g2d.setPaint(Color.BLACK);
			g2d.fill(arrow);
		}
		

		public int getIconWidth() {
			return 30;
		}
		

		public int getIconHeight() {
			return 20;
		}
	}
	

	private class MouseInputListener extends MouseAdapter {
		
		@Override
		public void mouseEntered(MouseEvent e) {
			hover = true;
			repaint();
		}
		

		@Override
		public void mouseExited(MouseEvent e) {
			hover = false;
			repaint();
		}
		
	}
	
}
