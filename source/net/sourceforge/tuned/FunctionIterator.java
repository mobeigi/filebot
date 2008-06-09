
package net.sourceforge.tuned;


import java.util.Iterator;
import java.util.NoSuchElementException;


public class FunctionIterator<S, T> implements Iterator<T> {
	
	/**
	 * A Function transforms one Object into another.
	 * 
	 * @param <S> type of source Objects
	 * @param <T> type Objects are transformed into
	 */
	public static interface Function<S, T> {
		
		/**
		 * Transform the given sourceValue into any kind of Object.
		 * 
		 * @param sourceValue - the Object to transform
		 * @return the transformed version of the object
		 */
		public T evaluate(S sourceValue);
	}
	
	private final Iterator<S> sourceIterator;
	private final Function<S, T> function;
	
	
	public FunctionIterator(Iterable<S> source, Function<S, T> function) {
		this(source.iterator(), function);
	}
	

	public FunctionIterator(Iterator<S> iterator, Function<S, T> function) {
		this.sourceIterator = iterator;
		this.function = function;
	}
	

	@Override
	public boolean hasNext() {
		try {
			return peekNext() != null;
		} catch (Exception e) {
			return true;
		}
	}
	

	@Override
	public T next() {
		if (!hasNext())
			throw new NoSuchElementException();
		
		try {
			return peekNext();
		} finally {
			cache = null;
			currentException = null;
		}
	}
	
	private T cache = null;
	private RuntimeException currentException = null;
	
	
	private T peekNext() {
		while (cache == null && (sourceIterator.hasNext() || currentException != null)) {
			if (currentException != null)
				throw currentException;
			
			try {
				cache = transform(sourceIterator.next());
			} catch (RuntimeException e) {
				currentException = e;
			}
		}
		
		return cache;
	}
	

	private T transform(S sourceValue) {
		return function.evaluate(sourceValue);
	}
	

	/**
	 * The remove operation is not supported by this implementation of <code>Iterator</code>.
	 * 
	 * @throws UnsupportedOperationException if this method is invoked.
	 * @see java.util.Iterator
	 */
	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
