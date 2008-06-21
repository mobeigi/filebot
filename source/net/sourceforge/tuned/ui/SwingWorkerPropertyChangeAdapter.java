
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
	

	public void state(PropertyChangeEvent evt) {
		switch ((StateValue) evt.getNewValue()) {
			case STARTED:
				started(evt);
				break;
			case DONE:
				done(evt);
				break;
		}
	}
	

	public void progress(PropertyChangeEvent evt) {
	}
	

	public void started(PropertyChangeEvent evt) {
	}
	

	public void done(PropertyChangeEvent evt) {
	}
	
}
