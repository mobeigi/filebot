
package net.sourceforge.tuned.ui;


import javax.swing.Icon;


public class NullLabelProvider<T extends Object> implements LabelProvider<T> {
	
	@Override
	public Icon getIcon(T value) {
		return null;
	}
	

	@Override
	public String getText(T value) {
		return value.toString();
	}
	
}
