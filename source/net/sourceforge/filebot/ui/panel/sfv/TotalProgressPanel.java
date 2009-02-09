
package net.sourceforge.filebot.ui.panel.sfv;


import static net.sourceforge.filebot.ui.panel.sfv.ChecksumComputationService.TASK_COUNT_PROPERTY;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import net.sourceforge.tuned.ui.TunedUtilities;


class TotalProgressPanel extends Box {
	
	private int millisToSetVisible = 200;
	
	private final JProgressBar progressBar = new JProgressBar(0, 0);
	
	private final ChecksumComputationService service;
	
	
	public TotalProgressPanel(ChecksumComputationService checksumComputationService) {
		super(BoxLayout.Y_AXIS);
		
		this.service = checksumComputationService;
		
		// invisible by default
		setVisible(false);
		
		progressBar.setStringPainted(true);
		progressBar.setBorderPainted(false);
		progressBar.setString("");
		
		setBorder(BorderFactory.createTitledBorder("Total Progress"));
		
		add(progressBar);
		
		checksumComputationService.addPropertyChangeListener(TASK_COUNT_PROPERTY, progressListener);
	}
	
	private final PropertyChangeListener progressListener = new PropertyChangeListener() {
		
		private Timer setVisibleTimer;
		
		
		public void propertyChange(PropertyChangeEvent evt) {
			final int completedTaskCount = service.getCompletedTaskCount();
			final int totalTaskCount = service.getTotalTaskCount();
			
			// invoke on EDT
			SwingUtilities.invokeLater(new Runnable() {
				
				@Override
				public void run() {
					if (completedTaskCount < totalTaskCount) {
						if (setVisibleTimer == null) {
							setVisibleTimer = TunedUtilities.invokeLater(millisToSetVisible, new Runnable() {
								
								@Override
								public void run() {
									setVisible(service.getTaskCount() > service.getCompletedTaskCount());
								}
							});
						}
					} else {
						if (setVisibleTimer != null) {
							setVisibleTimer.stop();
							setVisibleTimer = null;
						}
						
						// hide when not active
						setVisible(false);
					}
					
					progressBar.setValue(completedTaskCount);
					progressBar.setMaximum(totalTaskCount);
					
					progressBar.setString(completedTaskCount + " / " + totalTaskCount);
				};
			});
		}
	};
	
}
