
package net.sourceforge.tuned;


import java.util.Collection;
import java.util.Iterator;


public class DefaultProgressIterator<E> implements ProgressIterator<E> {
	
	private final Iterator<E> sourceIterator;
	private final int length;
	
	private int position = 0;
	
	
	public DefaultProgressIterator(Collection<E> source) {
		this.sourceIterator = source.iterator();
		this.length = source.size();
	}
	

	@Override
	public int getLength() {
		return length;
	}
	

	@Override
	public int getPosition() {
		return position;
	}
	

	@Override
	public boolean hasNext() {
		return sourceIterator.hasNext();
	}
	

	@Override
	public E next() {
		position++;
		return sourceIterator.next();
	}
	

	@Override
	public void remove() {
		// TODO Auto-generated method stub
		
	}
	
}
