
package net.sourceforge.filebot.ui.panel.rename;


import java.util.AbstractList;
import java.util.Collection;

import net.sourceforge.filebot.similarity.Match;
import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;


class RenameModel {
	
	private final EventList<Object> names = new BasicEventList<Object>();
	private final EventList<FileEntry> files = new BasicEventList<FileEntry>();
	
	
	public EventList<Object> names() {
		return names;
	}
	

	public EventList<FileEntry> files() {
		return files;
	}
	

	public void clear() {
		names.clear();
		files.clear();
	}
	

	public void setData(Collection<Match<Object, FileEntry>> matches) {
		// clear names and files
		clear();
		
		// add all matches
		for (Match<Object, FileEntry> match : matches) {
			names.add(match.getValue());
			files.add(match.getCandidate());
		}
	}
	

	public int matchCount() {
		return Math.min(names.size(), files.size());
	}
	

	public Match<Object, FileEntry> getMatch(int index) {
		if (index >= matchCount())
			throw new IndexOutOfBoundsException();
		
		return new Match<Object, FileEntry>(names.get(index), files.get(index));
	}
	

	public Collection<Match<Object, FileEntry>> matches() {
		return new AbstractList<Match<Object, FileEntry>>() {
			
			@Override
			public Match<Object, FileEntry> get(int index) {
				return getMatch(index);
			}
			

			@Override
			public int size() {
				return matchCount();
			}
			
		};
	}
}
