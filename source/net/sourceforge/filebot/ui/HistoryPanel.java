
package net.sourceforge.filebot.ui;


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
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import net.sourceforge.tuned.ui.HyperlinkLabel;


public class HistoryPanel extends JPanel {
	
	private JPanel grid = new JPanel(new GridLayout(0, 3, 15, 10));
	
	
	public HistoryPanel(String titleHeader, String infoHeader) {
		setLayout(new FlowLayout(FlowLayout.CENTER));
		
		JScrollPane scrollPane = new JScrollPane(grid, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		
		add(grid);
		
		setBackground(Color.WHITE);
		setOpaque(true);
		grid.setOpaque(false);
		
		JLabel titleLabel = new JLabel(titleHeader);
		JLabel infoLabel = new JLabel(infoHeader);
		JLabel durationLabel = new JLabel("Duration");
		
		Font font = titleLabel.getFont().deriveFont(Font.BOLD);
		
		titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
		infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
		durationLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		
		titleLabel.setFont(font);
		infoLabel.setFont(font);
		durationLabel.setFont(font);
		
		grid.add(titleLabel);
		grid.add(infoLabel);
		grid.add(durationLabel);
	}
	
	private final Border infoBorder = BorderFactory.createEmptyBorder(0, 0, 0, 10);
	
	
	public void add(String title, URL url, String info, long duration, Icon icon) {
		
		String durationString = NumberFormat.getInstance().format(duration) + " ms";
		
		JLabel titleLabel = (url != null) ? new HyperlinkLabel(title, url) : new JLabel(title);
		JLabel infoLabel = new JLabel(info);
		JLabel durationLabel = new JLabel(durationString);
		
		infoLabel.setBorder(infoBorder);
		
		titleLabel.setHorizontalAlignment(SwingConstants.LEFT);
		infoLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		durationLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		
		titleLabel.setIcon(icon);
		titleLabel.setIconTextGap(7);
		
		grid.add(titleLabel);
		grid.add(infoLabel);
		grid.add(durationLabel);
	}
	
}
