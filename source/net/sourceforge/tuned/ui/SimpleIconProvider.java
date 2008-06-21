
package net.sourceforge.tuned.ui;


import java.lang.reflect.Method;

import javax.swing.Icon;

import net.sourceforge.tuned.ExceptionUtil;


/**
 * <code>IconProvider</code> based on reflection.
 */
public class SimpleIconProvider<T> implements IconProvider<T> {
	
	private final Method getIconMethod;
	
	
	/**
	 * Same as <code>new SimpleIconProvider&lt;T&gt;(T.class)</code>.
	 * 
	 * @return new <code>IconProvider</code>
	 */
	public static <T> SimpleIconProvider<T> forClass(Class<T> type) {
		return new SimpleIconProvider<T>(type);
	}
	

	/**
	 * Create a new IconProvider which will use the <code>getIcon</code> method of the given
	 * class.
	 * 
	 * @param type a class with a <code>getIcon</code> method
	 */
	public SimpleIconProvider(Class<T> type) {
		this(type, "getIcon");
	}
	

	/**
	 * Create a new IconProvider which will use a specified method of a given class
	 * 
	 * @param type a class with the specified method
	 * @param getIcon a method name such as <code>getIcon</code>
	 */
	public SimpleIconProvider(Class<T> type, String getIcon) {
		try {
			getIconMethod = type.getMethod(getIcon);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	

	@Override
	public Icon getIcon(T value) {
		try {
			return (Icon) getIconMethod.invoke(value);
		} catch (Exception e) {
			throw ExceptionUtil.asRuntimeException(e);
		}
	}
	
}
