
package net.sourceforge.tuned;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


public class TestUtil {
	
	public static <T> List<List<T>> rotations(Collection<T> source) {
		List<List<T>> rotations = new ArrayList<List<T>>();
		
		for (int i = 0; i < source.size(); i++) {
			List<T> copy = new ArrayList<T>(source);
			Collections.rotate(copy, i);
			rotations.add(copy);
		}
		
		return rotations;
	}
	

	public static <T> List<T> asList(Iterator<T> iterator) {
		List<T> list = new ArrayList<T>();
		
		while (iterator.hasNext()) {
			list.add(iterator.next());
		}
		
		return list;
	}
	

	public static List<Object[]> asParameters(Object... parameterSet) {
		List<Object[]> list = new ArrayList<Object[]>();
		
		for (Object parameter : parameterSet) {
			list.add(new Object[] { parameter });
		}
		
		return list;
	}
}
