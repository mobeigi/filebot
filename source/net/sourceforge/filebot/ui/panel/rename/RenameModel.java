
package net.sourceforge.filebot.ui.panel.rename;


import java.io.File;
import java.util.AbstractList;
import java.util.Collection;

import net.sourceforge.filebot.similarity.Match;
import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;


class RenameModel {
	
	private final EventList<Object> names = new BasicEventList<Object>();
	private final EventList<File> files = new BasicEventList<File>();
	
	
	public EventList<Object> names() {
		return names;
	}
	

	public EventList<File> files() {
		return files;
	}
	

	public void clear() {
		names.clear();
		files.clear();
	}
	

	public int matchCount() {
		return Math.min(names.size(), files.size());
	}
	

	public Match<Object, File> getMatch(int index) {
		if (index >= matchCount())
			throw new IndexOutOfBoundsException();
		
		return new Match<Object, File>(names.get(index), files.get(index));
	}
	

	public Collection<Match<Object, File>> matches() {
		return new AbstractList<Match<Object, File>>() {
			
			@Override
			public Match<Object, File> get(int index) {
				return getMatch(index);
			}
			

			@Override
			public int size() {
				return matchCount();
			}
			
		};
	}
}
