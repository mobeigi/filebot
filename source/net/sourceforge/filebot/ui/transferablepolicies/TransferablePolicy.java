
package net.sourceforge.filebot.ui.transferablepolicies;


import java.awt.datatransfer.Transferable;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;


public abstract class TransferablePolicy {
	
	public abstract boolean accept(Transferable tr);
	

	public abstract void handleTransferable(Transferable tr, boolean add);
	
	private boolean enabled = true;
	
	private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
	
	public static final String ENABLED_PROPERTY = "enabled";
	
	
	public boolean isEnabled() {
		return enabled;
	}
	

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		
		firePropertyChange(ENABLED_PROPERTY, null, enabled);
	}
	

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(listener);
	}
	

	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
	}
	

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(listener);
	}
	

	public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
	}
	

	protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
		propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
	}
	

	public void firePropertyChange(PropertyChangeEvent evt) {
		propertyChangeSupport.firePropertyChange(evt);
	}
	

	public abstract String getDescription();
	
}
