
package net.sourceforge.filebot.ui.panel.rename;


import static net.sourceforge.filebot.FileBotUtilities.isInvalidFileName;

import java.awt.Component;
import java.text.Format;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.TransformedList;
import ca.odell.glazedlists.event.ListEvent;


public class NamesViewEventList extends TransformedList<Object, String> {
	
	private final List<String> names = new ArrayList<String>();
	
	private final Map<Class<?>, Format> formatMap = new HashMap<Class<?>, Format>();
	
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
	

	public void setFormat(Class<?> type, Format format) {
		if (format != null) {
			// insert new format for type
			formatMap.put(type, format);
		} else {
			// restore default format for type
			formatMap.remove(type);
		}
		
		updates.beginEvent(true);
		
		List<Integer> changes = new ArrayList<Integer>();
		
		// reformat all elements of the source list
		for (int i = 0; i < source.size(); i++) {
			String newValue = format(source.get(i));
			String oldValue = names.set(i, newValue);
			
			if (!newValue.equals(oldValue)) {
				updates.elementUpdated(i, oldValue, newValue);
				changes.add(i);
			}
		}
		
		submit(new IndexView<String>(names, changes));
		
		updates.commitEvent();
	}
	

	private String format(Object object) {
		for (Entry<Class<?>, Format> entry : formatMap.entrySet()) {
			if (entry.getKey().isInstance(object)) {
				return entry.getValue().format(object);
			}
		}
		
		return object.toString();
	}
	

	@Override
	public void listChanged(ListEvent<Object> listChanges) {
		EventList<Object> source = listChanges.getSourceList();
		List<Integer> changes = new ArrayList<Integer>();
		
		while (listChanges.next()) {
			int index = listChanges.getIndex();
			int type = listChanges.getType();
			
			switch (type) {
				case ListEvent.INSERT:
					names.add(index, format(source.get(index)));
					changes.add(index);
					break;
				case ListEvent.UPDATE:
					names.set(index, format(source.get(index)));
					changes.add(index);
					break;
				case ListEvent.DELETE:
					names.remove(index);
					break;
			}
		}
		
		submit(new IndexView<String>(names, changes));
		
		listChanges.reset();
		updates.forwardEvent(listChanges);
	}
	

	protected void submit(List<String> values) {
		List<Integer> issues = new ArrayList<Integer>();
		
		for (int i = 0; i < values.size(); i++) {
			if (isInvalidFileName(values.get(i))) {
				issues.add(i);
			}
		}
		
		if (issues.size() > 0) {
			// validate names
			ValidateNamesDialog.showDialog(parent, new IndexView<String>(values, issues));
		}
	}
	
	
	protected static class IndexView<E> extends AbstractList<E> {
		
		private final List<E> source;
		
		private final List<Integer> filter;
		
		
		public IndexView(List<E> source, List<Integer> filter) {
			this.source = source;
			this.filter = filter;
		}
		

		@Override
		public E get(int index) {
			return source.get(filter.get(index));
		}
		

		@Override
		public E set(int index, E element) {
			return source.set(filter.get(index), element);
		};
		

		@Override
		public int size() {
			return filter.size();
		}
		
	}
	
}
