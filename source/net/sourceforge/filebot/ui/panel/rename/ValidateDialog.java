
package net.sourceforge.filebot.ui.panel.rename;


import static java.util.Collections.*;
import static net.sourceforge.filebot.FileBotUtilities.*;
import static net.sourceforge.tuned.ui.TunedUtilities.*;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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


class ValidateDialog extends JDialog {
	
	private final JList list;
	
	private final Action validateAction = new ValidateAction();
	private final Action continueAction = new ContinueAction();
	private final Action cancelAction = new CancelAction();
	
	private String[] model;
	
	private boolean cancelled = true;
	
	
	public ValidateDialog(Window owner, Collection<String> source) {
		super(owner, "Invalid Names", ModalityType.DOCUMENT_MODAL);
		
		model = source.toArray(new String[0]);
		
		list = new JList(model);
		list.setEnabled(false);
		
		list.setCellRenderer(new HighlightListCellRenderer(INVALID_CHARACTERS_PATTERN, new CharacterHighlightPainter(new Color(0xFF4200), new Color(0xFF1200)), 4));
		
		JLabel label = new JLabel("Some names contain invalid characters:");
		
		JComponent content = (JComponent) getContentPane();
		
		content.setLayout(new MigLayout("insets dialog, nogrid, fill"));
		
		content.add(label, "wrap");
		content.add(new JScrollPane(list), "grow, wrap 2mm");
		
		content.add(new JButton(validateAction), "align center");
		content.add(new AlphaButton(continueAction), "gap related");
		content.add(new JButton(cancelAction), "gap 12mm");
		
		installAction(content, KeyStroke.getKeyStroke("released ESCAPE"), cancelAction);
		
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setLocationByPlatform(true);
		setSize(365, 280);
	}
	

	public List<String> getModel() {
		return unmodifiableList(Arrays.asList(model));
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
			putValue(NAME, "Validate");
			putValue(SMALL_ICON, ResourceManager.getIcon("dialog.continue"));
			putValue(SHORT_DESCRIPTION, "Remove invalid characters");
		}
		

		@Override
		public void actionPerformed(ActionEvent e) {
			// validate names
			for (int i = 0; i < model.length; i++) {
				model[i] = validateFileName(model[i]);
			}
			
			// update view
			list.repaint();
			
			continueAction.putValue(SMALL_ICON, getValue(SMALL_ICON));
			continueAction.putValue(ContinueAction.ALPHA, 1.0f);
			
			// disable action
			setEnabled(false);
		}
	}
	

	private class ContinueAction extends AbstractAction {
		
		public static final String ALPHA = "alpha";
		
		
		public ContinueAction() {
			putValue(NAME, "Continue");
			putValue(SMALL_ICON, ResourceManager.getIcon("dialog.continue.invalid"));
			putValue(ALPHA, 0.75f);
		}
		

		public void actionPerformed(ActionEvent e) {
			finish(false);
		}
	}
	

	protected class CancelAction extends AbstractAction {
		
		public CancelAction() {
			putValue(NAME, "Cancel");
			putValue(SMALL_ICON, ResourceManager.getIcon("dialog.cancel"));
		}
		

		public void actionPerformed(ActionEvent e) {
			finish(true);
		}
	}
	

	protected static class AlphaButton extends JButton {
		
		private float alpha;
		
		
		public AlphaButton(Action action) {
			super(action);
			
		}
		

		@Override
		protected void configurePropertiesFromAction(Action action) {
			super.configurePropertiesFromAction(action);
			
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
	
	
	public static boolean validate(Component parent, List<String> source) {
		IndexView<String> invalid = new IndexView<String>(source);
		
		for (int i = 0; i < source.size(); i++) {
			String name = source.get(i);
			
			if (isInvalidFileName(name)) {
				invalid.addIndex(i);
			}
		}
		
		if (invalid.isEmpty()) {
			// nothing to do
			return true;
		}
		
		ValidateDialog dialog = new ValidateDialog(getWindow(parent), invalid);
		
		// show and block
		dialog.setVisible(true);
		
		if (dialog.isCancelled()) {
			// no output
			return false;
		}
		
		List<String> valid = dialog.getModel();
		
		// validate source list via index view
		for (int i = 0; i < invalid.size(); i++) {
			invalid.set(i, valid.get(i));
		}
		
		return true;
	}
	
	
	private static class IndexView<E> extends AbstractList<E> {
		
		private final List<Integer> mapping = new ArrayList<Integer>();
		
		private final List<E> source;
		
		
		public IndexView(List<E> source) {
			this.source = source;
		}
		

		public boolean addIndex(int index) {
			return mapping.add(index);
		}
		

		@Override
		public E get(int index) {
			int sourceIndex = mapping.get(index);
			
			if (sourceIndex >= 0)
				return source.get(sourceIndex);
			
			return null;
		}
		

		@Override
		public E set(int index, E element) {
			return source.set(mapping.get(index), element);
		}
		

		@Override
		public int size() {
			return mapping.size();
		}
	}
	
}
