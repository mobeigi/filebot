
package net.sourceforge.tuned.ui;


import java.lang.reflect.Method;

import javax.swing.Icon;

import net.sourceforge.tuned.ExceptionUtil;


/**
 * <code>LabelProvider</code> based on reflection.
 */
public class SimpleLabelProvider<T> implements LabelProvider<T> {
	
	private final Method getIconMethod;
	private final Method getNameMethod;
	
	
	/**
	 * Same as <code>new SimpleLabelProvider&lt;T&gt;(T.class)</code>.
	 * 
	 * @return new <code>LabelProvider</code>
	 */
	public static <T> SimpleLabelProvider<T> forClass(Class<T> type) {
		return new SimpleLabelProvider<T>(type);
	}
	

	/**
	 * Create a new LabelProvider which will use the <code>getName</code> and
	 * <code>getIcon</code> method of the given class.
	 * 
	 * @param type a class that has a <code>getName</code> and a <code>getIcon</code> method
	 */
	public SimpleLabelProvider(Class<T> type) {
		this(type, "getName", "getIcon");
	}
	

	/**
	 * Create a new LabelProvider which will use a specified method of a given class
	 * 
	 * @param type a class with the specified method
	 * @param getName a method name such as <code>getName</code>
	 * @param getIcon a method name such as <code>getIcon</code>
	 */
	public SimpleLabelProvider(Class<T> type, String getName, String getIcon) {
		try {
			getNameMethod = type.getMethod(getName);
			getIconMethod = type.getMethod(getIcon);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	

	@Override
	public String getText(T value) {
		try {
			return (String) getNameMethod.invoke(value);
		} catch (Exception e) {
			throw ExceptionUtil.asRuntimeException(e);
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
