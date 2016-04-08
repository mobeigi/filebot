package net.filebot.ui.rename;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

class OriginalOrder<T> implements Comparator<T> {

	private Map<T, Integer> index;

	public OriginalOrder(Collection<T> values) {
		this.index = new HashMap<T, Integer>(values.size());

		int i = 0;
		for (T it : values) {
			index.put(it, i++);
		}
	}

	@Override
	public int compare(T a, T b) {
		return index.get(a).compareTo(index.get(b));
	}

}
