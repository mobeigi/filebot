
package net.sourceforge.tuned.ui;


import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;

import javax.swing.SwingWorker;
import javax.swing.Timer;



public class SwingWorkerProgressMonitor {
	
	public static final String PROPERTY_NOTE = "note";
	public static final String PROPERTY_PROGRESS_STRING = "progress string";
	
	private final SwingWorker<?, ?> worker;
	private final ProgressDialog progressDialog;
	
	private int millisToPopup = 2000;
	
	
	public SwingWorkerProgressMonitor(Window owner, SwingWorker<?, ?> worker) {
		this.worker = worker;
		
		progressDialog = new ProgressDialog(owner);
		
		worker.addPropertyChangeListener(listener);
		progressDialog.getCancelButton().addActionListener(cancelListener);
	}
	

	public ProgressDialog getProgressDialog() {
		return progressDialog;
	}
	

	public void setMillisToPopup(int millisToPopup) {
		this.millisToPopup = millisToPopup;
	}
	

	public int getMillisToPopup() {
		return millisToPopup;
	}
	
	private final SwingWorkerPropertyChangeAdapter listener = new SwingWorkerPropertyChangeAdapter() {
		
		private Timer popupTimer;
		
		
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			if (evt.getPropertyName().equals(PROPERTY_NOTE))
				note(evt);
			else if (evt.getPropertyName().equals(PROPERTY_PROGRESS_STRING))
				progressString(evt);
			else
				super.propertyChange(evt);
		}
		

		@Override
		public void started(PropertyChangeEvent evt) {
			popupTimer = TunedUtil.invokeLater(millisToPopup, new Runnable() {
				
				@Override
				public void run() {
					if (!worker.isDone()) {
						progressDialog.setVisible(true);
					}
				}
			});
		}
		

		@Override
		public void done(PropertyChangeEvent evt) {
			if (popupTimer != null) {
				popupTimer.stop();
			}
			
			progressDialog.close();
		}
		

		@Override
		public void progress(PropertyChangeEvent evt) {
			progressDialog.setProgressValue((Integer) evt.getNewValue());
		}
		

		public void progressString(PropertyChangeEvent evt) {
			progressDialog.setProgressString(evt.getNewValue().toString());
		}
		

		public void note(PropertyChangeEvent evt) {
			progressDialog.setNote(evt.getNewValue().toString());
		}
		
	};
	
	private final ActionListener cancelListener = new ActionListener() {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			worker.cancel(false);
		}
		
	};
	
}
