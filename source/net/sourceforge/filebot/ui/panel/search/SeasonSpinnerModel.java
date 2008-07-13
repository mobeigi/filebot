
package net.sourceforge.filebot.ui.panel.search;


import javax.swing.SpinnerNumberModel;


public class SeasonSpinnerModel extends SpinnerNumberModel {
	
	public static final int ALL_SEASONS = 0;
	
	
	public SeasonSpinnerModel() {
		super(ALL_SEASONS, ALL_SEASONS, Integer.MAX_VALUE, 1);
	}
	

	public int getSeason() {
		return getNumber().intValue();
	}
	

	public void lock(int maxSeason) {
		setMaximum(maxSeason);
	}
	

	public void unlock() {
		setMaximum(Integer.MAX_VALUE);
	}
	
}
