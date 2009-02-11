
package net.sourceforge.filebot.ui.panel.sfv;


import static net.sourceforge.filebot.ui.panel.sfv.ChecksumComputationService.TASK_COUNT_PROPERTY;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import net.miginfocom.swing.MigLayout;
import net.sourceforge.tuned.ui.TunedUtilities;


class TotalProgressPanel extends JComponent {
	
	private int millisToSetVisible = 200;
	
	private final JProgressBar progressBar = new JProgressBar(0, 0);
	
	private final ChecksumComputationService computationService;
	
	
	public TotalProgressPanel(ChecksumComputationService computationService) {
		this.computationService = computationService;
		
		setLayout(new MigLayout());
		
		// invisible by default
		setVisible(false);
		
		progressBar.setStringPainted(true);
		progressBar.setBorderPainted(false);
		progressBar.setString("");
		
		setBorder(BorderFactory.createTitledBorder("Total Progress"));
		
		add(progressBar, "growx");
		
		computationService.addPropertyChangeListener(TASK_COUNT_PROPERTY, progressListener);
	}
	
	private final PropertyChangeListener progressListener = new PropertyChangeListener() {
		
		private Timer setVisibleTimer;
		
		
		public void propertyChange(PropertyChangeEvent evt) {
			final int completedTaskCount = computationService.getCompletedTaskCount();
			final int totalTaskCount = computationService.getTotalTaskCount();
			
			// invoke on EDT
			SwingUtilities.invokeLater(new Runnable() {
				
				@Override
				public void run() {
					if (completedTaskCount < totalTaskCount) {
						if (setVisibleTimer == null) {
							setVisibleTimer = TunedUtilities.invokeLater(millisToSetVisible, new Runnable() {
								
								@Override
								public void run() {
									setVisible(computationService.getTaskCount() > computationService.getCompletedTaskCount());
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
					
					progressBar.setString(String.format("%d / %d", completedTaskCount, totalTaskCount));
				};
			});
		}
	};
	
}
