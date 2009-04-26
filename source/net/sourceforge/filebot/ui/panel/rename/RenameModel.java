
package net.sourceforge.filebot.ui.panel.rename;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingWorker;
import javax.swing.SwingWorker.StateValue;

import net.sourceforge.filebot.similarity.Match;
import net.sourceforge.tuned.ui.TunedUtilities;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.TransformedList;
import ca.odell.glazedlists.event.ListEvent;


public class RenameModel extends MatchModel<Object, File> {
	
	private final FormattedFutureEventList names = new FormattedFutureEventList();
	
	private final Map<Class<?>, MatchFormatter> formatters = new HashMap<Class<?>, MatchFormatter>();
	
	private final MatchFormatter defaultFormatter = new MatchFormatter() {
		
		@Override
		public boolean canFormat(Match<?, ?> match) {
			return true;
		}
		

		@Override
		public String preview(Match<?, ?> match) {
			return format(match);
		}
		

		@Override
		public String format(Match<?, ?> match) {
			return String.valueOf(match.getValue());
		}
	};
	
	
	public void useFormatter(Class<?> type, MatchFormatter formatter) {
		if (formatter != null) {
			formatters.put(type, formatter);
		} else {
			formatters.remove(type);
		}
		
		// reformat matches
		names.refresh();
	}
	

	public EventList<FormattedFuture> names() {
		return names;
	}
	

	public EventList<File> files() {
		return candidates();
	}
	

	public List<Match<String, File>> getMatchesForRenaming() {
		List<Match<String, File>> matches = new ArrayList<Match<String, File>>();
		
		for (int i = 0; i < size(); i++) {
			if (hasComplement(i) && names.get(i).isDone()) {
				matches.add(new Match<String, File>(names().get(i).toString(), files().get(i)));
			}
		}
		
		return matches;
	}
	

	private MatchFormatter getFormatter(Match<Object, File> match) {
		for (MatchFormatter formatter : formatters.values()) {
			if (formatter.canFormat(match)) {
				return formatter;
			}
		}
		
		return defaultFormatter;
	}
	
	
	private class FormattedFutureEventList extends TransformedList<Object, FormattedFuture> {
		
		private final List<FormattedFuture> futures = new ArrayList<FormattedFuture>();
		
		private final Executor backgroundFormatter = new ThreadPoolExecutor(0, 1, 5L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		
		
		public FormattedFutureEventList() {
			super(values());
			
			source.addListEventListener(this);
		}
		

		@Override
		public FormattedFuture get(int index) {
			return futures.get(index);
		}
		

		@Override
		protected boolean isWritable() {
			// can't write to source directly
			return false;
		}
		

		@Override
		public void add(int index, FormattedFuture value) {
			source.add(index, value.getMatch().getValue());
		}
		

		@Override
		public FormattedFuture set(int index, FormattedFuture value) {
			FormattedFuture obsolete = get(index);
			
			source.set(index, value.getMatch().getValue());
			
			return obsolete;
		}
		

		@Override
		public FormattedFuture remove(int index) {
			FormattedFuture obsolete = get(index);
			
			source.remove(index);
			
			return obsolete;
		}
		

		@Override
		public void listChanged(ListEvent<Object> listChanges) {
			updates.beginEvent(true);
			
			while (listChanges.next()) {
				int index = listChanges.getIndex();
				int type = listChanges.getType();
				
				if (type == ListEvent.INSERT || type == ListEvent.UPDATE) {
					Match<Object, File> match = getMatch(index);
					
					// create new future
					final FormattedFuture future = new FormattedFuture(match, getFormatter(match));
					
					// update data
					if (type == ListEvent.INSERT) {
						futures.add(index, future);
						updates.elementInserted(index, future);
					} else if (type == ListEvent.UPDATE) {
						// set new future, dispose old future
						FormattedFuture obsolete = futures.set(index, future);
						
						cancel(obsolete);
						
						// Don't update view immediately, to avoid irritating flickering, 
						// caused by a rapid succession of change events.
						// The worker may only need a couple of milliseconds to complete,
						// so the view will be notified of the change soon enough.
						TunedUtilities.invokeLater(50, new Runnable() {
							
							@Override
							public void run() {
								// task has not been started, no change events have been sent as of yet,
								// fire change event now
								if (future.getState() == StateValue.PENDING) {
									future.firePropertyChange("state", null, StateValue.PENDING);
								}
							}
						});
					}
					
					// observe and enqueue worker task
					submit(future);
				} else if (type == ListEvent.DELETE) {
					// remove future from data and formatter queue
					FormattedFuture obsolete = futures.remove(index);
					cancel(obsolete);
					updates.elementDeleted(index, obsolete);
				}
			}
			
			updates.commitEvent();
		}
		

		public void refresh() {
			updates.beginEvent(true);
			
			for (int i = 0; i < size(); i++) {
				FormattedFuture obsolete = futures.get(i);
				FormattedFuture future = new FormattedFuture(obsolete.getMatch(), getFormatter(obsolete.getMatch()));
				
				// replace and cancel old future
				cancel(futures.set(i, future));
				
				// submit new future
				submit(future);
				
				updates.elementUpdated(i, obsolete, future);
			}
			
			updates.commitEvent();
		}
		

		private void submit(FormattedFuture future) {
			// observe and enqueue worker task
			future.addPropertyChangeListener(futureListener);
			backgroundFormatter.execute(future);
		}
		

		private void cancel(FormattedFuture future) {
			// remove listener and cancel worker task
			future.removePropertyChangeListener(futureListener);
			future.cancel(true);
		}
		
		private final PropertyChangeListener futureListener = new PropertyChangeListener() {
			
			public void propertyChange(PropertyChangeEvent evt) {
				int index = futures.indexOf(evt.getSource());
				
				// sanity check
				if (index >= 0 && index < size()) {
					FormattedFuture future = (FormattedFuture) evt.getSource();
					
					updates.beginEvent(true);
					updates.elementUpdated(index, future, future);
					updates.commitEvent();
				}
			}
		};
	}
	

	public static class FormattedFuture extends SwingWorker<String, Void> {
		
		private final Match<Object, File> match;
		
		private final MatchFormatter formatter;
		
		private String display;
		
		
		private FormattedFuture(Match<Object, File> match, MatchFormatter formatter) {
			this.match = match;
			this.formatter = formatter;
			
			// initial display value
			this.display = formatter.preview(match);
		}
		

		public Match<Object, File> getMatch() {
			return match;
		}
		

		@Override
		protected String doInBackground() throws Exception {
			return formatter.format(match);
		}
		

		@Override
		protected void done() {
			if (isCancelled()) {
				return;
			}
			
			try {
				this.display = get();
			} catch (Exception e) {
				Logger.getLogger("global").log(Level.WARNING, e.getMessage(), e);
			}
		}
		

		@Override
		public String toString() {
			return display;
		}
	}
	
}
