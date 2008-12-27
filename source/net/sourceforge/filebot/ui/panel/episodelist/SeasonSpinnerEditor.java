
package net.sourceforge.filebot.ui.panel.episodelist;


import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


class SeasonSpinnerEditor extends JLabel implements ChangeListener {
	
	public SeasonSpinnerEditor(JSpinner spinner) {
		spinner.addChangeListener(this);
		setValueFromSpinner(spinner);
		
		setBackground(Color.WHITE);
		setOpaque(true);
	}
	

	public void stateChanged(ChangeEvent e) {
		setValueFromSpinner((JSpinner) e.getSource());
	}
	

	private void setValueFromSpinner(JSpinner spinner) {
		int season = ((SeasonSpinnerModel) spinner.getModel()).getSeason();
		
		if (season == SeasonSpinnerModel.ALL_SEASONS)
			setText("All Seasons");
		else
			setText(String.format("Season %d", season));
	}
}
