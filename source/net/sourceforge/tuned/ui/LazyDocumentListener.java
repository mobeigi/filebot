
package net.sourceforge.tuned.ui;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;


public abstract class LazyDocumentListener implements DocumentListener {
	
	private DocumentEvent lastEvent;
	
	private Timer timer;
	

	public LazyDocumentListener() {
		this(200);
	}
	

	public LazyDocumentListener(int delay) {
		if (delay >= 0) {
			timer = new Timer(delay, new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					update(lastEvent);
					
					// we don't need it anymore
					lastEvent = null;
				}
			});
			
			timer.setRepeats(false);
		}
	}
	

	public void defer(DocumentEvent e) {
		lastEvent = e;
		
		if (timer != null) {
			// defer update
			timer.restart();
		} else {
			// update immediately
			update(lastEvent);
		}
	}
	

	@Override
	public void changedUpdate(DocumentEvent e) {
		defer(e);
	}
	

	@Override
	public void insertUpdate(DocumentEvent e) {
		defer(e);
	}
	

	@Override
	public void removeUpdate(DocumentEvent e) {
		defer(e);
	}
	

	public abstract void update(DocumentEvent e);
	
}
