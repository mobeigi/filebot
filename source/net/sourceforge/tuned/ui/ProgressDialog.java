
package net.sourceforge.tuned.ui;


import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import net.miginfocom.swing.MigLayout;


public class ProgressDialog extends JDialog {
	
	private final JProgressBar progressBar = new JProgressBar(0, 100);
	private final JLabel iconLabel = new JLabel();
	private final JLabel headerLabel = new JLabel();
	private final JLabel noteLabel = new JLabel();
	
	private final JButton cancelButton;
	
	private boolean cancelled = false;
	
	
	public ProgressDialog(Window owner) {
		super(owner, ModalityType.DOCUMENT_MODAL);
		
		cancelButton = new JButton(cancelAction);
		
		addWindowListener(closeListener);
		
		headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD));
		progressBar.setStringPainted(true);
		
		JPanel c = (JPanel) getContentPane();
		
		c.setLayout(new MigLayout("insets panel, fill"));
		
		c.add(iconLabel, "spany 2, grow 0 0, gap right 1mm");
		c.add(headerLabel, "align left, wmax 70%, grow 100 0, wrap");
		c.add(noteLabel, "align left, wmax 70%, grow 100 0, wrap");
		c.add(progressBar, "spanx 2, gap top unrel, gap bottom unrel, grow, wrap");
		
		c.add(cancelButton, "spanx 2, align center");
		
		setSize(240, 155);
		
		setLocation(TunedUtil.getPreferredLocation(this));
	}
	

	public boolean isCancelled() {
		return cancelled;
	}
	

	public void setIcon(Icon icon) {
		iconLabel.setIcon(icon);
	}
	

	public void setNote(String text) {
		noteLabel.setText(text);
	}
	

	public void setHeader(String text) {
		headerLabel.setText(text);
	}
	

	public JProgressBar getProgressBar() {
		return progressBar;
	}
	

	public JButton getCancelButton() {
		return cancelButton;
	}
	

	public void close() {
		setVisible(false);
		dispose();
	}
	
	private final Action cancelAction = new AbstractAction("Cancel") {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			cancelled = true;
			close();
		}
		
	};
	
	private final WindowListener closeListener = new WindowAdapter() {
		
		@Override
		public void windowClosing(WindowEvent e) {
			cancelAction.actionPerformed(null);
		}
	};
	
}
