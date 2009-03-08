
package net.sourceforge.filebot.ui.panel.rename;


import static net.sourceforge.filebot.FileBotUtilities.isInvalidFileName;

import java.awt.Component;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.TransformedList;
import ca.odell.glazedlists.event.ListEvent;


public class NamesViewEventList extends TransformedList<Object, String> {
	
	private final List<String> names = new ArrayList<String>();
	
	private final Component parent;
	
	
	public NamesViewEventList(Component parent, EventList<Object> source) {
		super(source);
		
		this.parent = parent;
		
		// connect to source list
		source.addListEventListener(this);
	}
	

	@Override
	protected boolean isWritable() {
		return true;
	}
	

	@Override
	public String get(int index) {
		return names.get(index);
	}
	

	protected String format(Object object) {
		return object.toString();
	}
	

	@Override
	public void listChanged(ListEvent<Object> listChanges) {
		EventList<Object> source = listChanges.getSourceList();
		IndexView<String> newValues = new IndexView<String>(names);
		
		while (listChanges.next()) {
			int index = listChanges.getIndex();
			int type = listChanges.getType();
			
			switch (type) {
				case ListEvent.INSERT:
					names.add(index, format(source.get(index)));
					newValues.getIndexFilter().add(index);
					break;
				case ListEvent.UPDATE:
					names.set(index, format(source.get(index)));
					newValues.getIndexFilter().add(index);
					break;
				case ListEvent.DELETE:
					names.remove(index);
					break;
			}
		}
		
		submit(newValues);
		
		listChanges.reset();
		updates.forwardEvent(listChanges);
	}
	

	protected void submit(List<String> values) {
		IndexView<String> invalidValues = new IndexView<String>(values);
		
		for (int i = 0; i < values.size(); i++) {
			if (isInvalidFileName(values.get(i))) {
				invalidValues.getIndexFilter().add(i);
			}
		}
		
		if (invalidValues.size() > 0) {
			// validate names
			ValidateNamesDialog.showDialog(parent, invalidValues);
		}
	}
	
	
	protected static class IndexView<E> extends AbstractList<E> {
		
		private final List<E> source;
		
		private final List<Integer> indexFilter = new ArrayList<Integer>();
		
		
		public IndexView(List<E> source) {
			this.source = source;
		}
		

		public List<Integer> getIndexFilter() {
			return indexFilter;
		}
		

		@Override
		public E get(int index) {
			return source.get(indexFilter.get(index));
		}
		

		@Override
		public E set(int index, E element) {
			return source.set(indexFilter.get(index), element);
		};
		

		@Override
		public int size() {
			return indexFilter.size();
		}
		
	}
	
}
