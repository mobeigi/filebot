
package net.sourceforge.tuned.ui;


import java.awt.Window;
import java.awt.event.ActionEvent;

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
	
	private final Cancellable cancellable;
	
	
	public ProgressDialog(Window owner, Cancellable cancellable) {
		super(owner, ModalityType.DOCUMENT_MODAL);
		
		this.cancellable = cancellable;
		
		// disable window close button
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		
		headerLabel.setFont(headerLabel.getFont().deriveFont(18f));
		progressBar.setIndeterminate(true);
		progressBar.setStringPainted(true);
		JPanel c = (JPanel) getContentPane();
		c.setLayout(new MigLayout("insets dialog, nogrid, fill"));
		
		c.add(iconLabel, "h pref!, w pref!");
		c.add(headerLabel, "gap 3mm, wrap paragraph");
		c.add(progressBar, "hmin 25px, grow, wrap paragraph");
		
		c.add(new JButton(cancelAction), "align center");
		
		setSize(240, 155);
	}
	
	
	public void setIcon(Icon icon) {
		iconLabel.setIcon(icon);
	}
	
	
	public void setIndeterminate(boolean b) {
		progressBar.setIndeterminate(b);
	}
	
	
	public void setProgress(int value, int max) {
		progressBar.setIndeterminate(false);
		progressBar.setMinimum(0);
		progressBar.setValue(value);
		progressBar.setMaximum(max);
	}
	
	
	public void setNote(String text) {
		progressBar.setString(text);
	}
	
	
	@Override
	public void setTitle(String text) {
		super.setTitle(text);
		headerLabel.setText(text);
	}
	
	
	public void setWindowTitle(String text) {
		super.setTitle(text);
	}
	
	
	public void close() {
		setVisible(false);
		dispose();
	}
	
	
	protected final Action cancelAction = new AbstractAction("Cancel") {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			cancelAction.setEnabled(false);
			cancellable.cancel();
		}
	};
	
	
	public static interface Cancellable {
		
		boolean isCancelled();
		
		
		boolean cancel();
	}
	
}
