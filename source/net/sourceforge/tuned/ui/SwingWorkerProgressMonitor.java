
package net.sourceforge.tuned.ui;


import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;

import javax.swing.Icon;
import javax.swing.SwingWorker;
import javax.swing.Timer;


public class SwingWorkerProgressMonitor {
	
	public static final String PROPERTY_TITLE = "title";
	public static final String PROPERTY_NOTE = "note";
	public static final String PROPERTY_PROGRESS_STRING = "progress string";
	
	private final SwingWorker<?, ?> worker;
	private final ProgressDialog progressDialog;
	
	private int millisToPopup = 2000;
	
	
	public SwingWorkerProgressMonitor(Window owner, SwingWorker<?, ?> worker, Icon progressDialogIcon) {
		this.worker = worker;
		
		progressDialog = new ProgressDialog(owner);
		progressDialog.setIcon(progressDialogIcon);
		
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
		
		private Timer popupTimer = null;
		
		
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			if (evt.getPropertyName().equals(PROPERTY_PROGRESS_STRING))
				progressString(evt);
			else if (evt.getPropertyName().equals(PROPERTY_NOTE))
				note(evt);
			else if (evt.getPropertyName().equals(PROPERTY_TITLE))
				title(evt);
			else
				super.propertyChange(evt);
		}
		

		@Override
		protected void started(PropertyChangeEvent evt) {
			popupTimer = TunedUtil.invokeLater(millisToPopup, new Runnable() {
				
				@Override
				public void run() {
					if (!worker.isDone() && !progressDialog.isVisible()) {
						progressDialog.setVisible(true);
					}
				}
			});
		}
		

		@Override
		protected void done(PropertyChangeEvent evt) {
			if (popupTimer != null) {
				popupTimer.stop();
			}
			
			progressDialog.close();
		}
		

		@Override
		protected void progress(PropertyChangeEvent evt) {
			progressDialog.getProgressBar().setValue((Integer) evt.getNewValue());
		}
		

		protected void progressString(PropertyChangeEvent evt) {
			progressDialog.getProgressBar().setString(evt.getNewValue().toString());
		}
		

		protected void note(PropertyChangeEvent evt) {
			progressDialog.setNote(evt.getNewValue().toString());
		}
		

		protected void title(PropertyChangeEvent evt) {
			String title = evt.getNewValue().toString();
			
			progressDialog.setHeader(title);
			progressDialog.setTitle(title);
		}
		
	};
	
	private final ActionListener cancelListener = new ActionListener() {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			worker.cancel(false);
		}
		
	};
	
}
