
package net.sourceforge.filebot.ui.panel.episodelist;


import static net.sourceforge.filebot.ui.panel.episodelist.SeasonSpinnerModel.ALL_SEASONS;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


class SeasonSpinnerEditor extends JLabel implements ChangeListener {
	
	public SeasonSpinnerEditor(JSpinner spinner) {
		setHorizontalAlignment(RIGHT);
		
		spinner.addChangeListener(this);
		setValueFromSpinner(spinner);
		setBorder(BorderFactory.createEmptyBorder(1, 3, 1, 3));
		
		setBackground(Color.WHITE);
		setOpaque(true);
	}
	

	public void stateChanged(ChangeEvent e) {
		setValueFromSpinner((JSpinner) e.getSource());
	}
	

	private void setValueFromSpinner(JSpinner spinner) {
		int season = ((SeasonSpinnerModel) spinner.getModel()).getSeason();
		
		if (season == ALL_SEASONS)
			setText("All Seasons");
		else
			setText(String.format("Season %d", season));
	}
}
