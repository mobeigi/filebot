
package net.sourceforge.filebot.ui.panel.episodelist;


import javax.swing.SpinnerNumberModel;


class SeasonSpinnerModel extends SpinnerNumberModel {
	
	public static final int ALL_SEASONS = 0;
	
	public static final int MAX_VALUE = 99;
	
	private Number valueBeforeLock = null;
	
	
	public SeasonSpinnerModel() {
		super(ALL_SEASONS, ALL_SEASONS, MAX_VALUE, 1);
	}
	

	public int getSeason() {
		return getNumber().intValue();
	}
	

	public void spin(int steps) {
		int next = getSeason() + steps;
		
		if (next < ALL_SEASONS)
			next = ALL_SEASONS;
		else if (next > MAX_VALUE)
			next = MAX_VALUE;
		
		setValue(next);
	}
	

	public void lock(int value) {
		valueBeforeLock = getNumber();
		setMinimum(value);
		setMaximum(value);
		setValue(value);
	}
	

	public void unlock() {
		setMinimum(ALL_SEASONS);
		setMaximum(MAX_VALUE);
		
		if (valueBeforeLock != null) {
			setValue(valueBeforeLock);
			valueBeforeLock = null;
		}
	}
}
