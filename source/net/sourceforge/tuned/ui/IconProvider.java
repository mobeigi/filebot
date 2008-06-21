
package net.sourceforge.tuned.ui;


import javax.swing.Icon;


public interface IconProvider<T> {
	
	public Icon getIcon(T value);
	
}
