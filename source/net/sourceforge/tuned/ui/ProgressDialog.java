
package net.sourceforge.tuned.ui;


import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;


public class ProgressDialog extends JDialog {
	
	private final JProgressBar progressBar = new JProgressBar(0, 100);
	private final JLabel iconLabel = new JLabel();
	private final JLabel headerLabel = new JLabel();
	private final JLabel noteLabel = new JLabel();
	
	private final JButton cancelButton;
	
	private boolean cancelled = false;
	
	
	public ProgressDialog(Window owner) {
		super(owner, ModalityType.DOCUMENT_MODAL);
		
		if (!owner.getIconImages().isEmpty()) {
			setIconImages(owner.getIconImages());
		}
		
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(closeListener);
		
		cancelButton = new JButton(cancelAction);
		
		progressBar.setStringPainted(true);
		
		iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
		iconLabel.setVerticalAlignment(SwingConstants.CENTER);
		iconLabel.setBorder(new EmptyBorder(0, 2, 0, 10));
		
		Border labelBorder = new EmptyBorder(3, 0, 0, 0);
		headerLabel.setBorder(labelBorder);
		noteLabel.setBorder(labelBorder);
		
		JComponent c = (JComponent) getContentPane();
		
		c.setBorder(new EmptyBorder(5, 5, 5, 5));
		
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		buttonPanel.add(cancelButton);
		
		Box messageBox = Box.createVerticalBox();
		messageBox.add(headerLabel);
		messageBox.add(noteLabel);
		messageBox.add(Box.createVerticalGlue());
		
		JPanel messagePanel = new JPanel(new BorderLayout());
		messagePanel.add(iconLabel, BorderLayout.WEST);
		messagePanel.add(messageBox, BorderLayout.CENTER);
		
		JPanel progressBarPanel = new JPanel(new BorderLayout());
		progressBarPanel.add(progressBar, BorderLayout.CENTER);
		progressBarPanel.setBorder(new EmptyBorder(8, 12, 3, 12));
		
		Box progressBox = Box.createVerticalBox();
		progressBox.add(messagePanel);
		progressBox.add(progressBarPanel);
		
		c.add(progressBox, BorderLayout.CENTER);
		c.add(buttonPanel, BorderLayout.SOUTH);
		
		setSize(240, 138);
		setResizable(false);
		
		setLocation(TunedUtil.getPreferredLocation(this));
		
		// Shortcut Escape
		TunedUtil.registerActionForKeystroke(c, KeyStroke.getKeyStroke("released ESCAPE"), cancelAction);
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
	

	public void setProgressMaximum(int n) {
		progressBar.setMaximum(n);
	}
	

	public void setProgressMinimum(int n) {
		progressBar.setMinimum(n);
	}
	

	public void setProgressValue(int n) {
		progressBar.setValue(n);
	}
	

	public void setProgressString(String text) {
		progressBar.setString(text);
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
			cancelled = true;
			close();
		}
	};
}
