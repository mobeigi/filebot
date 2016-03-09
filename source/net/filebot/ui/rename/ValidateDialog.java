package net.filebot.ui.rename;

import static java.util.Collections.*;
import static net.filebot.Logging.*;
import static net.filebot.Settings.*;
import static net.filebot.util.FileUtilities.*;
import static net.filebot.util.ui.SwingUI.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.text.BadLocationException;

import net.filebot.ResourceManager;
import net.miginfocom.swing.MigLayout;

class ValidateDialog extends JDialog {

	private final JList list;

	private File[] model;

	private boolean cancelled = true;

	public ValidateDialog(Window owner, Collection<File> source) {
		super(owner, "Invalid Names", ModalityType.DOCUMENT_MODAL);

		model = source.toArray(new File[0]);

		list = new JList(model);
		list.setEnabled(false);

		list.setCellRenderer(new HighlightListCellRenderer(ILLEGAL_CHARACTERS, new CharacterHighlightPainter(new Color(0xFF4200), new Color(0xFF1200)), 4) {

			@Override
			protected void updateHighlighter() {
				textComponent.getHighlighter().removeAllHighlights();

				Matcher matcher = pattern.matcher(textComponent.getText());
				File file = new File(textComponent.getText());

				// highlight path components separately to ignore "illegal characters" that are either path separators or part of the drive letter (e.g. ':' in 'E:')
				for (File element : listPath(file)) {
					int limit = element.getPath().length();
					matcher.region(limit - element.getName().length(), limit);

					while (matcher.find()) {
						try {
							textComponent.getHighlighter().addHighlight(matcher.start(0), matcher.end(0), highlightPainter);
						} catch (BadLocationException e) {
							// should not happen
							debug.log(Level.SEVERE, e.getMessage(), e);
						}
					}
				}
			}
		});

		JLabel label = new JLabel("Some names contain invalid characters:");

		JComponent content = (JComponent) getContentPane();

		content.setLayout(new MigLayout("insets dialog, nogrid, fill", "", "[pref!][fill][pref!]"));

		content.add(label, "wrap");
		content.add(new JScrollPane(list), "grow, wrap 2mm");

		content.add(new JButton(validateAction), "align center");
		content.add(new JButton(continueAction), "gap related");
		content.add(new JButton(cancelAction), "gap 12mm");

		installAction(content, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), cancelAction);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setMinimumSize(new Dimension(365, 280));
		pack();
	}

	public List<File> getModel() {
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
				// remove illegal characters
				model[i] = validateFilePath(model[i]);
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

	public static boolean validate(Component parent, List<File> source) {
		IndexView<File> invalidFilePaths = new IndexView<File>(source);

		for (int i = 0; i < source.size(); i++) {
			// invalid file names are also invalid file paths
			if (isInvalidFilePath(source.get(i)) && !isUnixFS()) {
				invalidFilePaths.addIndex(i);
			}
		}

		// check if there is anything to do in the first place
		if (invalidFilePaths.isEmpty()) {
			return true;
		}

		ValidateDialog dialog = new ValidateDialog(getWindow(parent), invalidFilePaths);
		dialog.setLocation(getOffsetLocation(dialog.getOwner()));

		// show and block
		dialog.setVisible(true);

		if (dialog.isCancelled()) {
			// no output
			return false;
		}

		List<File> validatedFilePaths = dialog.getModel();

		// validate source list via index view
		for (int i = 0; i < invalidFilePaths.size(); i++) {
			invalidFilePaths.set(i, validatedFilePaths.get(i));
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
