
package net.sourceforge.filebot.ui.episodelist;


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
	

	@Override
	public Integer getMinimum() {
		return (Integer) super.getMinimum();
	}
	

	@Override
	public Integer getMaximum() {
		return (Integer) super.getMaximum();
	}
	

	public void spin(int steps) {
		int next = getSeason() + steps;
		
		if (next < getMinimum())
			next = getMinimum();
		else if (next > getMaximum())
			next = getMaximum();
		
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
