
package net.sourceforge.filebot.ui.panel.rename;


import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;

import net.sourceforge.filebot.FileBotUtil;
import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.filebot.ui.panel.rename.entry.ListEntry;
import net.sourceforge.tuned.ui.SimpleListModel;


public class ValidateNamesDialog extends JDialog {
	
	private final List<ListEntry<?>> entries;
	
	private boolean cancelled = true;
	
	private final ValidateAction validateAction = new ValidateAction();
	private final ContinueAction continueAction = new ContinueAction();
	private final CancelAction cancelAction = new CancelAction();
	
	
	public ValidateNamesDialog(Window owner, List<ListEntry<?>> entries) {
		super(owner, "Invalid Names", ModalityType.DOCUMENT_MODAL);
		
		this.entries = entries;
		
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		JList list = new JList(new SimpleListModel(entries));
		list.setEnabled(false);
		
		list.setCellRenderer(new HighlightListCellRenderer(FileBotUtil.INVALID_CHARACTERS_PATTERN, new CharacterHighlightPainter(Color.decode("#FF4200"), Color.decode("#FF1200")), 4, true));
		
		JLabel label = new JLabel("Some names contain invalid characters:");
		
		JComponent c = (JComponent) getContentPane();
		
		int border = 5;
		c.setBorder(BorderFactory.createEmptyBorder(border, border, border, border));
		c.setLayout(new BorderLayout(border, border));
		
		JPanel listPanel = new JPanel(new BorderLayout());
		
		listPanel.add(new JScrollPane(list), BorderLayout.CENTER);
		
		Box buttonBox = Box.createHorizontalBox();
		buttonBox.setBorder(new EmptyBorder(5, 5, 5, 5));
		
		buttonBox.add(Box.createHorizontalGlue());
		buttonBox.add(new JButton(validateAction));
		buttonBox.add(Box.createHorizontalStrut(10));
		buttonBox.add(new AlphaButton(continueAction));
		
		buttonBox.add(Box.createHorizontalStrut(40));
		buttonBox.add(new JButton(cancelAction));
		buttonBox.add(Box.createHorizontalGlue());
		
		c.add(label, BorderLayout.NORTH);
		c.add(listPanel, BorderLayout.CENTER);
		c.add(buttonBox, BorderLayout.SOUTH);
		
		setLocation(FileBotUtil.getPreferredLocation(this));
		
		// Shortcut Escape
		FileBotUtil.registerActionForKeystroke(c, KeyStroke.getKeyStroke("released ESCAPE"), cancelAction);
		
		pack();
	}
	

	public boolean isCancelled() {
		return cancelled;
	}
	

	private void finish(boolean cancelled) {
		this.cancelled = cancelled;
		
		setVisible(false);
		dispose();
	}
	
	
	private class ValidateAction extends AbstractAction {
		
		public ValidateAction() {
			super("Validate", ResourceManager.getIcon("dialog.continue"));
			putValue(SHORT_DESCRIPTION, "Remove invalid characters");
		}
		

		@Override
		public void actionPerformed(ActionEvent e) {
			for (ListEntry<?> entry : entries) {
				String validatedName = FileBotUtil.validateFileName(entry.getName());
				entry.setName(validatedName);
			}
			
			setEnabled(false);
			
			continueAction.putValue(SMALL_ICON, getValue(SMALL_ICON));
			continueAction.putValue(ContinueAction.ALPHA, 1.0f);
			
			repaint();
		}
	};
	

	private class ContinueAction extends AbstractAction {
		
		public static final String ALPHA = "Alpha";
		
		
		public ContinueAction() {
			super("Continue", ResourceManager.getIcon("dialog.continue.invalid"));
			putValue(ALPHA, 0.75f);
		}
		

		public void actionPerformed(ActionEvent e) {
			finish(false);
		}
	};
	

	private class CancelAction extends AbstractAction {
		
		public CancelAction() {
			super("Cancel", ResourceManager.getIcon("dialog.cancel"));
		}
		

		public void actionPerformed(ActionEvent e) {
			finish(true);
		}
	};
	

	private static class AlphaButton extends JButton {
		
		private float alpha;
		
		
		public AlphaButton(Action action) {
			super(action);
			alpha = getAlpha(action);
		}
		

		@Override
		protected void actionPropertyChanged(Action action, String propertyName) {
			super.actionPropertyChanged(action, propertyName);
			
			if (propertyName == ContinueAction.ALPHA) {
				alpha = getAlpha(action);
			}
		}
		

		private float getAlpha(Action action) {
			Object value = action.getValue(ContinueAction.ALPHA);
			
			if (value instanceof Float) {
				return (Float) value;
			}
			
			return 1.0f;
		}
		

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2d = (Graphics2D) g;
			g2d.setComposite(AlphaComposite.SrcOver.derive(alpha));
			super.paintComponent(g2d);
		}
	}
	
}
