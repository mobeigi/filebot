
package net.sourceforge.tuned.ui;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.SwingWorker;


public abstract class SwingWorkerPropertyChangeAdapter implements PropertyChangeListener {
	
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals("progress"))
			progress(evt);
		else if (evt.getNewValue().equals(SwingWorker.StateValue.STARTED))
			started(evt);
		else if (evt.getNewValue().equals(SwingWorker.StateValue.DONE))
			done(evt);
	}
	

	public void started(PropertyChangeEvent evt) {
	}
	

	public void done(PropertyChangeEvent evt) {
	}
	

	public void progress(PropertyChangeEvent evt) {
	}
}
