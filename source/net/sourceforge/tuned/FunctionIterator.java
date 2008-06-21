
package net.sourceforge.tuned;


import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;


public class FunctionIterator<S, T> implements ProgressIterator<T> {
	
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
		public T evaluate(S sourceValue) throws Exception;
	}
	
	private final Iterator<S> sourceIterator;
	private final Function<S, T> function;
	private final int length;
	
	private int position = 0;
	
	
	public FunctionIterator(Collection<S> source, Function<S, T> function) {
		this(source.iterator(), source.size(), function);
	}
	

	//TODO TEST case!!! for piped functions -> correct progress
	public FunctionIterator(ProgressIterator<S> iterator, Function<S, T> function) {
		this(iterator, iterator.getLength(), function);
	}
	

	public FunctionIterator(Iterator<S> iterator, int length, Function<S, T> function) {
		this.sourceIterator = iterator;
		this.length = length;
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
			} catch (Exception e) {
				currentException = ExceptionUtil.asRuntimeException(e);
			}
			
			position++;
		}
		
		return cache;
	}
	

	private T transform(S sourceValue) throws Exception {
		return function.evaluate(sourceValue);
	}
	

	@Override
	public int getPosition() {
		if (sourceIterator instanceof FunctionIterator) {
			return ((ProgressIterator<?>) sourceIterator).getPosition();
		}
		
		return position;
	}
	

	@Override
	public int getLength() {
		return length;
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
