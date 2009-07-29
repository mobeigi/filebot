
package net.sourceforge.tuned.ui;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;


public abstract class LazyDocumentListener implements DocumentListener {
	
	private DocumentEvent lastEvent;
	
	private final Timer timer;
	

	public LazyDocumentListener() {
		this(200);
	}
	

	public LazyDocumentListener(int delay) {
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
	

	private void defer(DocumentEvent e) {
		lastEvent = e;
		timer.restart();
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
