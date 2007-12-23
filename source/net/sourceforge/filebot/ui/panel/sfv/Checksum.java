
package net.sourceforge.filebot.ui.panel.sfv;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;

import net.sourceforge.tuned.ui.SwingWorkerPropertyChangeAdapter;


public class Checksum {
	
	private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
	
	public static final String STATE_PROPERTY = "STATE_PROPERTY";
	public static final String PROGRESS_PROPERTY = "PROGRESS_PROPERTY";
	
	private Long checksum = null;
	private State state = State.PENDING;
	private ChecksumComputationTask computationTask = null;
	
	
	public static enum State {
		PENDING, INPROGRESS, READY;
	}
	
	
	public Checksum(long checksum) {
		setChecksum(checksum);
		setState(State.READY);
	}
	

	public Checksum(String checksumString) {
		this(Long.parseLong(checksumString, 16));
	}
	

	public Checksum(File file) {
		computationTask = new ChecksumComputationTask(file);
		computationTask.addPropertyChangeListener(new ComputationTaskPropertyChangeListener());
		
		ChecksumComputationExecutor.getInstance().execute(computationTask);
	}
	

	public String getChecksumString() {
		StringBuffer buffer = new StringBuffer(8);
		
		buffer.append(Long.toHexString(checksum).toUpperCase());
		
		while (buffer.length() < 8) {
			buffer.insert(0, "0");
		}
		
		return buffer.toString();
	}
	

	public Long getChecksum() {
		return checksum;
	}
	

	public synchronized void setChecksum(Long checksum) {
		this.checksum = checksum;
		setState(State.READY);
		computationTask = null;
	}
	

	public State getState() {
		return state;
	}
	

	private void setState(State state) {
		this.state = state;
		propertyChangeSupport.firePropertyChange(STATE_PROPERTY, null, state);
	}
	

	public Integer getProgress() {
		switch (state) {
			case PENDING:
				return 0;
			case READY:
				return 100;
			default:
				return computationTask.getProgress();
		}
	}
	

	public synchronized void cancelComputationTask() {
		if (computationTask == null)
			return;
		
		computationTask.cancel(false);
	}
	
	
	private class ComputationTaskPropertyChangeListener extends SwingWorkerPropertyChangeAdapter {
		
		@Override
		public void progress(PropertyChangeEvent evt) {
			propertyChangeSupport.firePropertyChange(PROGRESS_PROPERTY, null, evt.getNewValue());
		}
		

		@Override
		public void started(PropertyChangeEvent evt) {
			setState(State.INPROGRESS);
		}
		

		@Override
		public void done(PropertyChangeEvent evt) {
			try {
				if (!computationTask.isCancelled()) {
					setChecksum(computationTask.get());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(listener);
	}
	

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(listener);
	}
	

	@Override
	public String toString() {
		switch (state) {
			case PENDING:
				return state.toString();
			case INPROGRESS:
				return state.toString();
			case READY:
				return getChecksumString();
			default:
				return null;
		}
	}
	
}
