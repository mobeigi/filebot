
package net.sourceforge.filebot.ui;


import java.awt.Color;
import java.awt.Font;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;
import net.sourceforge.tuned.ui.HyperlinkLabel;


public class HistoryPanel extends JPanel {
	
	private final List<JLabel> columnHeaders = new ArrayList<JLabel>(3);
	
	
	public HistoryPanel() {
		super(new MigLayout("fillx, insets 10 30 10 50, wrap 3"));
		
		setBackground(Color.WHITE);
		setOpaque(true);
		
		setupHeader();
	}
	

	protected void setupHeader() {
		for (int i = 0; i < 3; i++) {
			JLabel columnHeader = new JLabel();
			
			columnHeader.setFont(columnHeader.getFont().deriveFont(Font.BOLD));
			
			columnHeaders.add(columnHeader);
			add(columnHeader, (i == 0) ? "align left, gapbefore 20" : "align right, gapafter 20");
		}
	}
	

	public void setColumnHeader(int index, String text) {
		columnHeaders.get(index).setText(text);
	}
	

	public void add(String column1, URI link, Icon icon, String column2, String column3) {
		JLabel label1 = (link != null) ? new HyperlinkLabel(column1, link) : new JLabel(column1);
		JLabel label2 = new JLabel(column2, SwingConstants.RIGHT);
		JLabel label3 = new JLabel(column3, SwingConstants.RIGHT);
		
		label1.setIcon(icon);
		label1.setIconTextGap(7);
		
		add(label1, "align left");
		
		// set minimum with to 100px so the text is aligned to the right,
		// even though the whole label is centered
		add(label2, "align center, wmin 100");
		
		add(label3, "align right");
	}
}
