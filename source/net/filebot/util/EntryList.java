package net.filebot.util;

import static java.util.Collections.*;

import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class EntryList<K, V> extends AbstractMap<K, V> {

	private final List<Entry<K, V>> entryList = new ArrayList<Entry<K, V>>();

	public EntryList(Iterable<? extends K> keys, Iterable<? extends V> values) {
		Iterator<? extends K> keySeq = keys != null ? keys.iterator() : emptyIterator();
		Iterator<? extends V> valueSeq = values != null ? values.iterator() : emptyIterator();

		while (keySeq.hasNext() || valueSeq.hasNext()) {
			K key = keySeq.hasNext() ? keySeq.next() : null;
			V value = valueSeq.hasNext() ? valueSeq.next() : null;
			entryList.add(new SimpleImmutableEntry<K, V>(key, value));
		}
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return new AbstractSet<Entry<K, V>>() {

			@Override
			public Iterator<Entry<K, V>> iterator() {
				return entryList.iterator();
			}

			@Override
			public int size() {
				return entryList.size();
			}
		};
	}

	@Override
	public List<V> values() {
		return new AbstractList<V>() {

			@Override
			public V get(int index) {
				return entryList.get(index).getValue();
			}

			@Override
			public int size() {
				return entryList.size();
			}
		};
	}

	@Override
	public int size() {
		return entryList.size();
	}

}
