
package net.sourceforge.tuned.ui;


import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Calendar;

import javax.swing.BoundedRangeModel;
import javax.swing.JComponent;


public class ProgressIndicator extends JComponent {
	
	private BoundedRangeModel model = null;
	
	private boolean indeterminate = false;
	
	private float indeterminateRadius = 4.0f;
	private int indeterminateShapeCount = 1;
	
	private float progressStrokeWidth = 4.5f;
	private float remainingStrokeWidth = 2f;
	
	private Stroke progressStroke = new BasicStroke(progressStrokeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND);
	private Stroke remainingStroke = new BasicStroke(remainingStrokeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND);
	
	private Color progressColor = Color.orange;
	private Color remainingColor = new Color(0f, 0f, 0f, 0.25f);
	
	private Color textColor = new Color(0x5F5F5F);
	
	private boolean paintText = true;
	private boolean paintBackground = false;
	
	private final Rectangle2D frame = new Rectangle2D.Double();
	private final Arc2D arc = new Arc2D.Double();
	private final Ellipse2D circle = new Ellipse2D.Double();
	
	private final Dimension baseSize = new Dimension(32, 32);
	
	
	public ProgressIndicator() {
		this(null);
	}
	

	public ProgressIndicator(BoundedRangeModel model) {
		this.model = model;
		
		indeterminate = (model == null);
		
		setFont(new Font(Font.DIALOG, Font.BOLD, 8));
	}
	

	public double getProgress() {
		if (model == null)
			return 0;
		
		double total = model.getMaximum() - model.getMinimum();
		double current = model.getValue() - model.getMinimum();
		
		return current / total;
	}
	

	@Override
	public void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		
		double a = Math.min(getWidth(), getHeight());
		
		g2d.scale(a / baseSize.width, a / baseSize.height);
		
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		if (paintBackground) {
			frame.setFrame(0, 0, baseSize.width, baseSize.height);
			
			g2d.setPaint(getBackground());
			circle.setFrame(frame);
			g2d.fill(circle);
		}
		
		double inset = Math.max(Math.max(remainingStrokeWidth, progressStrokeWidth), indeterminateRadius);
		frame.setFrame(inset, inset, baseSize.width - inset * 2 - 1, baseSize.height - inset * 2 - 1);
		
		if (!indeterminate) {
			paintProgress(g2d);
		} else {
			paintIndeterminate(g2d);
		}
	}
	

	protected void paintProgress(Graphics2D g2d) {
		
		double progress = getProgress();
		
		// remaining circle
		circle.setFrame(frame);
		
		g2d.setStroke(remainingStroke);
		g2d.setPaint(remainingColor);
		
		g2d.draw(circle);
		
		// progress circle
		arc.setArc(frame, 90, progress * 360 * -1, Arc2D.OPEN);
		
		g2d.setStroke(progressStroke);
		g2d.setPaint(progressColor);
		
		g2d.draw(arc);
		
		if (paintText) {
			// text
			g2d.setFont(getFont());
			g2d.setPaint(textColor);
			
			String text = String.format("%d%%", (int) (100 * progress));
			Rectangle2D textBounds = g2d.getFontMetrics().getStringBounds(text, g2d);
			
			g2d.drawString(text, (float) (frame.getCenterX() - textBounds.getX() - (textBounds.getWidth() / 2f) + 0.5f), (float) (frame.getCenterY() - textBounds.getY() - (textBounds.getHeight() / 2)));
		}
	}
	

	protected void paintIndeterminate(Graphics2D g2d) {
		circle.setFrame(frame);
		
		g2d.setStroke(remainingStroke);
		g2d.setPaint(remainingColor);
		
		g2d.draw(circle);
		
		Point2D center = new Point2D.Double(frame.getCenterX(), frame.getMinY());
		
		circle.setFrameFromCenter(center, new Point2D.Double(center.getX() + indeterminateRadius, center.getY() + indeterminateRadius));
		
		g2d.setStroke(progressStroke);
		g2d.setPaint(progressColor);
		
		Calendar now = Calendar.getInstance();
		
		double theta = getTheta(now.get(Calendar.MILLISECOND), now.getMaximum(Calendar.MILLISECOND));
		g2d.rotate(theta, frame.getCenterX(), frame.getCenterY());
		
		theta = getTheta(1, indeterminateShapeCount);
		
		for (int i = 0; i < indeterminateShapeCount; i++) {
			g2d.rotate(theta, frame.getCenterX(), frame.getCenterY());
			g2d.fill(circle);
		}
		
	}
	

	private double getTheta(int value, int max) {
		return ((double) value / max) * 2 * Math.PI;
	}
	

	public BoundedRangeModel getModel() {
		return model;
	}
	

	public void setModel(BoundedRangeModel model) {
		this.model = model;
	}
	

	public boolean isIndeterminate() {
		return indeterminate;
	}
	

	public void setIndeterminate(boolean indeterminate) {
		this.indeterminate = indeterminate;
	}
	

	public void setIndeterminateRadius(float indeterminateRadius) {
		this.indeterminateRadius = indeterminateRadius;
	}
	

	public void setIndeterminateShapeCount(int indeterminateShapeCount) {
		this.indeterminateShapeCount = indeterminateShapeCount;
	}
	

	public void setProgressColor(Color progressColor) {
		this.progressColor = progressColor;
	}
	

	public void setRemainingColor(Color remainingColor) {
		this.remainingColor = remainingColor;
	}
	

	public void setTextColor(Color textColor) {
		this.textColor = textColor;
	}
	

	public void setPaintBackground(boolean paintBackground) {
		this.paintBackground = paintBackground;
	}
	

	public void setPaintText(boolean paintString) {
		this.paintText = paintString;
	}
	
}
