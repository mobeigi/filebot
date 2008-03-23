
package net.sourceforge.filebot.ui.panel.sfv;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JProgressBar;
import javax.swing.Timer;
import javax.swing.border.Border;


class TotalProgressPanel extends Box {
	
	private JProgressBar progressBar = new JProgressBar(0, 0);
	
	
	public TotalProgressPanel() {
		super(BoxLayout.Y_AXIS);
		
		// start invisible
		super.setVisible(false);
		
		progressBar.setStringPainted(true);
		progressBar.setBorderPainted(false);
		progressBar.setString("");
		
		Border margin = BorderFactory.createEmptyBorder(5, 5, 4, 8);
		Border title = BorderFactory.createTitledBorder("Total Progress");
		
		setBorder(BorderFactory.createCompoundBorder(margin, title));
		
		add(progressBar);
		ChecksumComputationService.getService().addPropertyChangeListener(executorListener);
	}
	
	private PropertyChangeListener executorListener = new PropertyChangeListener() {
		
		public void propertyChange(PropertyChangeEvent evt) {
			
			String property = evt.getPropertyName();
			
			if (property == ChecksumComputationService.ACTIVE_PROPERTY) {
				Boolean active = (Boolean) evt.getNewValue();
				
				if (active) {
					new SetVisibleTimer().start();
				}
			} else if (property == ChecksumComputationService.REMAINING_TASK_COUNT_PROPERTY) {
				
				int taskCount = ChecksumComputationService.getService().getActiveSessionTaskCount();
				int progress = taskCount - ChecksumComputationService.getService().getRemainingTaskCount();
				
				progressBar.setValue(progress);
				progressBar.setMaximum(taskCount);
				
				progressBar.setString(progressBar.getValue() + " / " + progressBar.getMaximum());
			}
			
			if (!ChecksumComputationService.getService().isActive()) {
				setVisible(false);
			}
		}
	};
	
	
	private class SetVisibleTimer extends Timer implements ActionListener {
		
		private static final int millisToSetVisible = 200;
		
		
		public SetVisibleTimer() {
			super(millisToSetVisible, null);
			addActionListener(this);
			setRepeats(false);
		}
		

		public void actionPerformed(ActionEvent e) {
			setVisible(ChecksumComputationService.getService().isActive());
		}
	}
	
}
