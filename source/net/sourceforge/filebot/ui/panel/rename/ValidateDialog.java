
package net.sourceforge.filebot.ui.panel.rename;


import static java.util.Collections.*;
import static net.sourceforge.tuned.FileUtilities.*;
import static net.sourceforge.tuned.ui.TunedUtilities.*;

import java.awt.Color;
import java.awt.Component;
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
	
	private String[] model;
	
	private boolean cancelled = true;
	

	public ValidateDialog(Window owner, Collection<String> source) {
		super(owner, "Invalid Names", ModalityType.DOCUMENT_MODAL);
		
		model = source.toArray(new String[0]);
		
		list = new JList(model);
		list.setEnabled(false);
		
		list.setCellRenderer(new HighlightListCellRenderer(ILLEGAL_CHARACTERS, new CharacterHighlightPainter(new Color(0xFF4200), new Color(0xFF1200)), 4));
		
		JLabel label = new JLabel("Some names contain invalid characters:");
		
		JComponent content = (JComponent) getContentPane();
		
		content.setLayout(new MigLayout("insets dialog, nogrid, fill"));
		
		content.add(label, "wrap");
		content.add(new JScrollPane(list), "grow, wrap 2mm");
		
		content.add(new JButton(validateAction), "align center");
		content.add(new JButton(continueAction), "gap related");
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
	

	private final Action validateAction = new AbstractAction("Validate", ResourceManager.getIcon("dialog.continue")) {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			// validate names
			for (int i = 0; i < model.length; i++) {
				model[i] = validateFileName(model[i]);
			}
			
			// update view
			list.repaint();
			
			// switch icon
			continueAction.putValue(SMALL_ICON, getValue(SMALL_ICON));
			
			// disable this action
			setEnabled(false);
		}
	};
	
	private final Action continueAction = new AbstractAction("Continue", ResourceManager.getIcon("dialog.continue.invalid")) {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			finish(false);
		}
	};
	
	private final Action cancelAction = new AbstractAction("Cancel", ResourceManager.getIcon("dialog.cancel")) {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			finish(true);
		}
	};
	

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
