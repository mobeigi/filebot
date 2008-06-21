
package net.sourceforge.filebot.ui;


import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.net.URI;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import net.sourceforge.tuned.ui.HyperlinkLabel;


public class HistoryPanel extends JPanel {
	
	private final JPanel grid = new JPanel(new GridLayout(0, 3, 15, 10));
	
	private final JLabel columnHeader1 = new JLabel();
	private final JLabel columnHeader2 = new JLabel();
	private final JLabel columnHeader3 = new JLabel();
	
	
	public HistoryPanel() {
		super(new FlowLayout(FlowLayout.CENTER));
		
		setBackground(Color.WHITE);
		setOpaque(true);
		grid.setOpaque(false);
		
		Font font = columnHeader1.getFont().deriveFont(Font.BOLD);
		
		columnHeader1.setHorizontalAlignment(SwingConstants.CENTER);
		columnHeader2.setHorizontalAlignment(SwingConstants.CENTER);
		columnHeader3.setHorizontalAlignment(SwingConstants.RIGHT);
		
		columnHeader1.setFont(font);
		columnHeader2.setFont(font);
		columnHeader3.setFont(font);
		
		grid.add(columnHeader1);
		grid.add(columnHeader2);
		grid.add(columnHeader3);
		
		add(grid);
	}
	

	public void setColumnHeader1(String text) {
		columnHeader1.setText(text);
	}
	

	public void setColumnHeader2(String text) {
		columnHeader2.setText(text);
	}
	

	public void setColumnHeader3(String text) {
		columnHeader3.setText(text);
	}
	

	public void add(String column1, URI link, Icon icon, String column2, String column3) {
		JLabel label1 = (link != null) ? new HyperlinkLabel(column1, link) : new JLabel(column1);
		JLabel label2 = new JLabel(column2);
		JLabel label3 = new JLabel(column3);
		
		label1.setHorizontalAlignment(SwingConstants.LEFT);
		label2.setHorizontalAlignment(SwingConstants.RIGHT);
		label3.setHorizontalAlignment(SwingConstants.RIGHT);
		
		label1.setIcon(icon);
		label1.setIconTextGap(7);
		
		label2.setBorder(new EmptyBorder(0, 0, 0, 10));
		
		grid.add(label1);
		grid.add(label2);
		grid.add(label3);
	}
}
