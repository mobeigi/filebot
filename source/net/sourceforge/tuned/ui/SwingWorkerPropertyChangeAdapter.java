
package net.sourceforge.tuned.ui;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.SwingWorker.StateValue;


public abstract class SwingWorkerPropertyChangeAdapter implements PropertyChangeListener {
	
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals("progress"))
			progress(evt);
		else if (evt.getPropertyName().equals("state"))
			state(evt);
	}
	

	protected void state(PropertyChangeEvent evt) {
		switch ((StateValue) evt.getNewValue()) {
			case STARTED:
				started(evt);
				break;
			case DONE:
				done(evt);
				break;
		}
	}
	

	protected void progress(PropertyChangeEvent evt) {
	}
	

	protected void started(PropertyChangeEvent evt) {
	}
	

	protected void done(PropertyChangeEvent evt) {
	}
	
}
