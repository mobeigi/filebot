
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
		ChecksumComputationExecutor.getInstance().addPropertyChangeListener(executorListener);
	}
	
	private PropertyChangeListener executorListener = new PropertyChangeListener() {
		
		public void propertyChange(PropertyChangeEvent evt) {
			ChecksumComputationExecutor executor = ChecksumComputationExecutor.getInstance();
			String property = evt.getPropertyName();
			
			if (property == ChecksumComputationExecutor.ACTIVE_PROPERTY) {
				Boolean active = (Boolean) evt.getNewValue();
				setVisible(active);
				return;
			}
			
			if (property == ChecksumComputationExecutor.PAUSED_PROPERTY) {
				Boolean paused = (Boolean) evt.getNewValue();
				
				if (paused) {
					progressBar.setString("Updating ...");
				}
				
				return;
			}
			
			if (property == ChecksumComputationExecutor.ACTIVE_SESSION_TASK_COUNT_PROPERTY) {
				progressBar.setMaximum(executor.getActiveSessionTaskCount());
			}
			
			if (property == ChecksumComputationExecutor.REMAINING_TASK_COUNT_PROPERTY) {
				int progress = executor.getActiveSessionTaskCount() - executor.getRemainingTaskCount();
				progressBar.setValue(progress);
			}
			
			progressBar.setString(progressBar.getValue() + " / " + progressBar.getMaximum());
		}
	};
	
	
	@Override
	public void setVisible(boolean flag) {
		if (flag) {
			new SetVisibleTimer().start();
		} else {
			super.setVisible(false);
		}
	}
	
	private int millisToSetVisible = 200;
	
	
	private class SetVisibleTimer extends Timer implements ActionListener {
		
		public SetVisibleTimer() {
			super(millisToSetVisible, null);
			addActionListener(this);
			setRepeats(false);
		}
		

		public void actionPerformed(ActionEvent e) {
			if (ChecksumComputationExecutor.getInstance().isActive())
				TotalProgressPanel.super.setVisible(true);
		}
	}
	
}
