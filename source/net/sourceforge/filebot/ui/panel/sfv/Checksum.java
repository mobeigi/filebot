
package net.sourceforge.filebot.ui.panel.sfv;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.tuned.ui.SwingWorkerPropertyChangeAdapter;


public class Checksum {
	
	private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
	
	public static final String STATE_PROPERTY = "state";
	public static final String PROGRESS_PROPERTY = "progress";
	
	private Long checksum = null;
	private State state = State.PENDING;
	private ChecksumComputationTask computationTask = null;
	private String errorMessage = null;
	
	
	public static enum State {
		PENDING,
		INPROGRESS,
		READY,
		ERROR;
	}
	
	
	public Checksum(long checksum) {
		setChecksum(checksum);
		setState(State.READY);
	}
	

	public Checksum(String checksumString) {
		this(Long.parseLong(checksumString, 16));
	}
	

	protected Checksum(ChecksumComputationTask computationTask) {
		this.computationTask = computationTask;
		this.computationTask.addPropertyChangeListener(new ComputationTaskPropertyChangeListener());
	}
	

	public String getChecksumString() {
		return String.format("%08x", checksum).toUpperCase();
	}
	

	public Long getChecksum() {
		return checksum;
	}
	

	public synchronized void setChecksum(Long checksum) {
		this.checksum = checksum;
		setState(State.READY);
		
		computationTask = null;
	}
	

	public synchronized void setChecksumError(Exception exception) {
		// get root cause
		Throwable cause = exception;
		
		while (cause.getCause() != null)
			cause = cause.getCause();
		
		errorMessage = cause.getMessage();
		setState(State.ERROR);
		
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
		if (state == State.INPROGRESS)
			return computationTask.getProgress();
		
		return null;
	}
	

	public String getErrorMessage() {
		return errorMessage;
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
				// might happen if file system is corrupt (e.g. CRC errors)
				setChecksumError(e);
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING, e.getMessage());
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
		if (state == State.READY)
			return getChecksumString();
		
		if (state == State.ERROR)
			return getErrorMessage();
		
		return state.toString();
	}
	
}
