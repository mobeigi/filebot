
package net.sourceforge.filebot.ui.panel.search;


import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.net.URL;
import java.text.NumberFormat;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import net.sourceforge.tuned.ui.HyperlinkLabel;


class HistoryPanel extends JPanel {
	
	private JPanel grid = new JPanel(new GridLayout(0, 3, 15, 10));
	
	
	public HistoryPanel() {
		setLayout(new FlowLayout(FlowLayout.CENTER));
		
		add(grid);
		
		setBackground(Color.WHITE);
		setOpaque(true);
		grid.setOpaque(false);
		
		JLabel linkLabel = new JLabel("Show");
		JLabel numberLabel = new JLabel("Number of Episodes");
		JLabel durationLabel = new JLabel("Duration");
		
		Font font = linkLabel.getFont().deriveFont(Font.BOLD);
		
		linkLabel.setHorizontalAlignment(SwingConstants.CENTER);
		numberLabel.setHorizontalAlignment(SwingConstants.CENTER);
		durationLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		
		linkLabel.setFont(font);
		numberLabel.setFont(font);
		durationLabel.setFont(font);
		
		grid.add(linkLabel);
		grid.add(numberLabel);
		grid.add(durationLabel);
	}
	
	private Border numberBorder = BorderFactory.createEmptyBorder(0, 0, 0, 10);
	
	
	public void add(String show, URL url, int number, long duration, Icon searchEngineIcon) {
		String numberString = null;
		
		if (number > 0)
			numberString = Integer.toString(number) + " episodes";
		else
			numberString = "No episodes found";
		
		String durationString = NumberFormat.getInstance().format(duration) + " ms";
		
		JLabel linkLabel = new HyperlinkLabel(show, url);
		JLabel numberLabel = new JLabel(numberString);
		JLabel durationLabel = new JLabel(durationString);
		
		numberLabel.setBorder(numberBorder);
		
		linkLabel.setHorizontalAlignment(SwingConstants.LEFT);
		numberLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		durationLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		
		linkLabel.setIcon(searchEngineIcon);
		linkLabel.setIconTextGap(7);
		
		grid.add(linkLabel);
		grid.add(numberLabel);
		grid.add(durationLabel);
	}
	
}
