
package net.sourceforge.filebot.ui.panel.sfv;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.Map;

import net.sourceforge.tuned.ExceptionUtilities;
import net.sourceforge.tuned.ui.SwingWorkerPropertyChangeAdapter;


public class ChecksumCell {
	
	private final String name;
	private final File root;
	
	private Map<HashType, String> hashes;
	private ChecksumComputationTask task;
	private Throwable error;
	
	
	public static enum State {
		PENDING,
		PROGRESS,
		READY,
		ERROR
	}
	
	
	public ChecksumCell(String name, File root, Map<HashType, String> hashes) {
		this.name = name;
		this.root = root;
		this.hashes = hashes;
	}
	

	public ChecksumCell(String name, File root, ChecksumComputationTask computationTask) {
		this.name = name;
		this.root = root;
		this.task = computationTask;
		
		// forward property change events
		task.addPropertyChangeListener(new SwingWorkerPropertyChangeAdapter() {
			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				super.propertyChange(evt);
				
				pcs.firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
			}
			

			@Override
			protected void done(PropertyChangeEvent evt) {
				try {
					hashes = task.get();
				} catch (Exception e) {
					error = ExceptionUtilities.getRootCause(e);
				} finally {
					task = null;
				}
			}
		});
	}
	

	public String getName() {
		return name;
	}
	

	public File getRoot() {
		return root;
	}
	

	public String getChecksum(HashType type) {
		if (hashes != null)
			return hashes.get(type);
		
		return null;
	}
	

	public ChecksumComputationTask getTask() {
		return task;
	}
	

	public Throwable getError() {
		return error;
	}
	

	public State getState() {
		if (hashes != null)
			return State.READY;
		if (error != null)
			return State.ERROR;
		
		switch (task.getState()) {
			case PENDING:
				return State.PENDING;
			default:
				return State.PROGRESS;
		}
	}
	

	public void dispose() {
		// clear property change support first
		for (PropertyChangeListener listener : pcs.getPropertyChangeListeners()) {
			pcs.removePropertyChangeListener(listener);
		}
		
		if (task != null) {
			task.cancel(true);
		}
		
		hashes = null;
		error = null;
		task = null;
		pcs = null;
	}
	

	@Override
	public String toString() {
		return String.format("%s %s", name, hashes);
	}
	
	private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	
	
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		pcs.addPropertyChangeListener(listener);
	}
	

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		pcs.removePropertyChangeListener(listener);
	}
	
}
