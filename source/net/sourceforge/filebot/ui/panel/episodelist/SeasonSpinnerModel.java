
package net.sourceforge.filebot.ui.panel.episodelist;


import javax.swing.SpinnerNumberModel;


public class SeasonSpinnerModel extends SpinnerNumberModel {
	
	public static final int ALL_SEASONS = 0;
	
	
	public SeasonSpinnerModel() {
		super(ALL_SEASONS, ALL_SEASONS, Integer.MAX_VALUE, 1);
	}
	

	public Integer getSeason() {
		return getNumber().intValue();
	}
	

	public void spin(int steps) {
		int next = getSeason() + steps;
		
		if (next < ALL_SEASONS)
			next = ALL_SEASONS;
		
		setValue(next);
	}
	

	public void lock(boolean lock) {
		if (lock) {
			setValue(ALL_SEASONS);
			setMaximum(ALL_SEASONS);
		} else {
			setMaximum(Integer.MAX_VALUE);
		}
	}
	
}
