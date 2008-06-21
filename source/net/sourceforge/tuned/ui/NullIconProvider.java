
package net.sourceforge.tuned.ui;


import javax.swing.Icon;


public class NullIconProvider<T extends Object> implements IconProvider<T> {
	
	@Override
	public Icon getIcon(Object value) {
		return null;
	}
	
}
