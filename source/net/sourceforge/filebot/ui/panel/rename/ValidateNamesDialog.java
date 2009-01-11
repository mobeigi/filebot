
package net.sourceforge.filebot.ui.panel.rename;


import static net.sourceforge.filebot.FileBotUtil.INVALID_CHARACTERS_PATTERN;
import static net.sourceforge.filebot.FileBotUtil.validateFileName;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.Collection;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

import net.miginfocom.swing.MigLayout;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.tuned.ui.ArrayListModel;
import net.sourceforge.tuned.ui.TunedUtil;


public class ValidateNamesDialog extends JDialog {
	
	private final Collection<StringEntry> entries;
	
	private boolean cancelled = true;
	
	protected final Action validateAction = new ValidateAction();
	protected final Action continueAction = new ContinueAction();
	protected final Action cancelAction = new CancelAction();
	
	
	public ValidateNamesDialog(Window owner, Collection<StringEntry> entries) {
		super(owner, "Invalid Names", ModalityType.DOCUMENT_MODAL);
		
		this.entries = entries;
		
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		JList list = new JList(new ArrayListModel(entries));
		list.setEnabled(false);
		
		list.setCellRenderer(new HighlightListCellRenderer(INVALID_CHARACTERS_PATTERN, new CharacterHighlightPainter(new Color(0xFF4200), new Color(0xFF1200)), 4));
		
		JLabel label = new JLabel("Some names contain invalid characters:");
		
		JComponent c = (JComponent) getContentPane();
		
		c.setLayout(new MigLayout("insets dialog, nogrid, fill"));
		
		c.add(label, "wrap");
		c.add(new JScrollPane(list), "grow, wrap 2mm");
		
		c.add(new JButton(validateAction), "align center");
		c.add(new AlphaButton(continueAction), "gap related");
		c.add(new JButton(cancelAction), "gap 12mm");
		
		setSize(365, 280);
		
		TunedUtil.putActionForKeystroke(c, KeyStroke.getKeyStroke("released ESCAPE"), cancelAction);
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
			for (StringEntry entry : entries) {
				entry.setValue(validateFileName(entry.getValue()));
			}
			
			setEnabled(false);
			
			continueAction.putValue(SMALL_ICON, getValue(SMALL_ICON));
			continueAction.putValue(ContinueAction.ALPHA, 1.0f);
			
			// render list entries again to display changes
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
	

	protected class CancelAction extends AbstractAction {
		
		public CancelAction() {
			super("Cancel", ResourceManager.getIcon("dialog.cancel"));
		}
		

		public void actionPerformed(ActionEvent e) {
			finish(true);
		}
	};
	

	protected static class AlphaButton extends JButton {
		
		private float alpha;
		
		
		public AlphaButton(Action action) {
			super(action);
			alpha = getAlpha(action);
		}
		

		@Override
		protected void actionPropertyChanged(Action action, String propertyName) {
			super.actionPropertyChanged(action, propertyName);
			
			if (propertyName.equals(ContinueAction.ALPHA)) {
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
