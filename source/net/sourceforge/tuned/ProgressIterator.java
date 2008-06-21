
package net.sourceforge.tuned;


import java.util.Iterator;


public interface ProgressIterator<E> extends Iterator<E> {
	
	public int getPosition();
	

	public int getLength();
	
}
