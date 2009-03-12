
package net.sourceforge.filebot.ui.panel.rename;


import java.util.AbstractList;
import java.util.Collection;

import net.sourceforge.filebot.similarity.Match;
import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;


class RenameModel<N, V> {
	
	private final EventList<N> names;
	private final EventList<V> files;
	
	
	public RenameModel(EventList<N> names, EventList<V> files) {
		this.names = names;
		this.files = files;
	}
	

	public EventList<N> names() {
		return names;
	}
	

	public EventList<V> files() {
		return files;
	}
	

	public void clear() {
		names.clear();
		files.clear();
	}
	

	public int matchCount() {
		return Math.min(names.size(), files.size());
	}
	

	public Match<N, V> getMatch(int index) {
		if (index >= matchCount())
			throw new IndexOutOfBoundsException();
		
		return new Match<N, V>(names.get(index), files.get(index));
	}
	

	public Collection<Match<N, V>> matches() {
		return new AbstractList<Match<N, V>>() {
			
			@Override
			public Match<N, V> get(int index) {
				return getMatch(index);
			}
			

			@Override
			public int size() {
				return matchCount();
			}
			
		};
	}
	

	@SuppressWarnings("unchecked")
	public static <S, V> RenameModel<S, V> create() {
		return new RenameModel<S, V>((EventList<S>) new BasicEventList<Object>(), (EventList<V>) new BasicEventList<Object>());
	}
	

	public static <S, V> RenameModel<S, V> wrap(EventList<S> names, EventList<V> values) {
		return new RenameModel<S, V>(names, values);
	}
	
}
