package net.filebot.ui.list;

import static java.awt.Font.*;
import static java.lang.Math.*;
import static net.filebot.ui.NotificationLogging.*;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JSpinner.NumberEditor;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;

import net.filebot.ResourceManager;
import net.filebot.format.ExpressionFormat;
import net.filebot.similarity.SeriesNameMatcher;
import net.filebot.ui.FileBotList;
import net.filebot.ui.FileBotListExportHandler;
import net.filebot.ui.transfer.LoadAction;
import net.filebot.ui.transfer.SaveAction;
import net.filebot.util.ExceptionUtilities;
import net.filebot.util.ui.SwingUI;
import net.miginfocom.swing.MigLayout;

public class ListPanel extends JComponent {

	private FileBotList<String> list = new FileBotList<String>();

	private JTextField textField = new JTextField("Name - {i}", 30);
	private SpinnerNumberModel fromSpinnerModel = new SpinnerNumberModel(1, 0, Integer.MAX_VALUE, 1);
	private SpinnerNumberModel toSpinnerModel = new SpinnerNumberModel(20, 0, Integer.MAX_VALUE, 1);

	public ListPanel() {
		list.setTitle("Title");

		textField.setFont(new Font(MONOSPACED, PLAIN, 11));

		list.setTransferablePolicy(new FileListTransferablePolicy(list));
		list.setExportHandler(new FileBotListExportHandler(list));

		list.getRemoveAction().setEnabled(true);

		JSpinner fromSpinner = new JSpinner(fromSpinnerModel);
		JSpinner toSpinner = new JSpinner(toSpinnerModel);

		fromSpinner.setEditor(new NumberEditor(fromSpinner, "#"));
		toSpinner.setEditor(new NumberEditor(toSpinner, "#"));

		setLayout(new MigLayout("nogrid, fill, insets dialog", "align center", "[pref!, center][fill]"));

		add(new JLabel("Pattern:"), "gapbefore indent");
		add(textField, "gap related, wmin 2cm, sizegroupy editor");
		add(new JLabel("From:"), "gap 5mm");
		add(fromSpinner, "gap related, wmax 14mm, sizegroup spinner, sizegroupy editor");
		add(new JLabel("To:"), "gap 5mm");
		add(toSpinner, "gap related, wmax 14mm, sizegroup spinner, sizegroupy editor");
		add(new JButton(createAction), "gap 7mm, gapafter indent, wrap paragraph");

		add(list, "grow");

		// panel with buttons that will be added inside the list component
		JPanel buttonPanel = new JPanel(new MigLayout("insets 1.2mm, nogrid, fill", "align center"));
		buttonPanel.add(new JButton(new LoadAction(list.getTransferablePolicy())));
		buttonPanel.add(new JButton(new SaveAction(list.getExportHandler())), "gap related");

		list.add(buttonPanel, BorderLayout.SOUTH);

		SwingUI.installAction(this, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), createAction);
	}

	private AbstractAction createAction = new AbstractAction("Create", ResourceManager.getIcon("action.export")) {

		@Override
		public void actionPerformed(ActionEvent evt) {

			// clear selection
			list.getListComponent().clearSelection();

			int from = fromSpinnerModel.getNumber().intValue();
			int to = toSpinnerModel.getNumber().intValue();

			try {
				ExpressionFormat format = new ExpressionFormat(textField.getText());

				// pad episode numbers with zeros (e.g. %02d) so all numbers have the same number of digits
				NumberFormat numberFormat = NumberFormat.getIntegerInstance();
				numberFormat.setMinimumIntegerDigits(max(2, Integer.toString(max(from, to)).length()));
				numberFormat.setGroupingUsed(false);

				List<String> names = new ArrayList<String>();

				int min = min(from, to);
				int max = max(from, to);

				for (int i = min; i <= max; i++) {
					Bindings bindings = new SimpleBindings();

					// strings
					bindings.put("i", numberFormat.format(i));

					// numbers
					bindings.put("index", i);
					bindings.put("from", from);
					bindings.put("to", to);

					names.add(format.format(bindings));
				}

				if (signum(to - from) < 0) {
					Collections.reverse(names);
				}

				// try to match title from the first five names
				Collection<String> title = new SeriesNameMatcher().matchAll((names.size() < 5 ? names : names.subList(0, 4)).toArray(new String[0]));

				list.setTitle(title.isEmpty() ? "List" : title.iterator().next());

				list.getModel().clear();
				list.getModel().addAll(names);
			} catch (Exception e) {
				UILogger.log(Level.WARNING, ExceptionUtilities.getMessage(e), e);
			}
		}
	};

}
