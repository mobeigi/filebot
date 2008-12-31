
package net.sourceforge.tuned.ui;


import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Calendar;

import javax.swing.JComponent;
import javax.swing.Timer;


public class ProgressIndicator extends JComponent {
	
	private float radius = 4.0f;
	private int shapeCount = 3;
	
	private float strokeWidth = 2f;
	private Stroke stroke = new BasicStroke(strokeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND);
	
	private Color progressShapeColor = Color.orange;
	private Color backgroundShapeColor = new Color(0f, 0f, 0f, 0.25f);
	
	private final Rectangle2D frame = new Rectangle2D.Double();
	private final Ellipse2D circle = new Ellipse2D.Double();
	
	private final Dimension baseSize = new Dimension(32, 32);
	
	private Timer updateTimer = new Timer(20, new ActionListener() {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			repaint();
		}
	});
	
	
	public ProgressIndicator() {
		setPreferredSize(baseSize);
		
		addComponentListener(new ComponentAdapter() {
			
			@Override
			public void componentShown(ComponentEvent e) {
				startAnimation();
			}
			

			@Override
			public void componentHidden(ComponentEvent e) {
				stopAnimation();
			}
		});
	}
	

	@Override
	public void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		
		double a = Math.min(getWidth(), getHeight());
		
		g2d.scale(a / baseSize.width, a / baseSize.height);
		
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		frame.setFrame(radius, radius, baseSize.width - radius * 2 - 1, baseSize.height - radius * 2 - 1);
		
		paintShapes(g2d);
	}
	

	private void paintShapes(Graphics2D g2d) {
		circle.setFrame(frame);
		
		g2d.setStroke(stroke);
		g2d.setPaint(backgroundShapeColor);
		
		g2d.draw(circle);
		
		Point2D center = new Point2D.Double(frame.getCenterX(), frame.getMinY());
		
		circle.setFrameFromCenter(center, new Point2D.Double(center.getX() + radius, center.getY() + radius));
		
		g2d.setStroke(stroke);
		g2d.setPaint(progressShapeColor);
		
		Calendar now = Calendar.getInstance();
		
		double theta = getTheta(now.get(Calendar.MILLISECOND), now.getMaximum(Calendar.MILLISECOND));
		g2d.rotate(theta, frame.getCenterX(), frame.getCenterY());
		
		theta = getTheta(1, shapeCount);
		
		for (int i = 0; i < shapeCount; i++) {
			g2d.rotate(theta, frame.getCenterX(), frame.getCenterY());
			g2d.fill(circle);
		}
	}
	

	private double getTheta(int value, int max) {
		return ((double) value / max) * 2 * Math.PI;
	}
	

	public void startAnimation() {
		updateTimer.restart();
	}
	

	public void stopAnimation() {
		updateTimer.stop();
	}
	

	public void setShapeCount(int indeterminateShapeCount) {
		this.shapeCount = indeterminateShapeCount;
	}
	
}
