
package net.sourceforge.filebot.ui.panel.list;


import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.signum;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.JSpinner.NumberEditor;

import net.miginfocom.swing.MigLayout;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.similarity.SeriesNameMatcher;
import net.sourceforge.filebot.ui.FileBotList;
import net.sourceforge.filebot.ui.FileBotListExportHandler;
import net.sourceforge.filebot.ui.FileBotPanel;
import net.sourceforge.filebot.ui.FileTransferableMessageHandler;
import net.sourceforge.filebot.ui.transfer.LoadAction;
import net.sourceforge.filebot.ui.transfer.SaveAction;
import net.sourceforge.tuned.MessageHandler;
import net.sourceforge.tuned.ui.TunedUtilities;


public class ListPanel extends FileBotPanel {
	
	private static final String INDEX_VARIABLE = "<i>";
	
	private FileBotList<String> list = new FileBotList<String>();
	
	private JTextField textField = new JTextField(String.format("Name - %s", INDEX_VARIABLE), 25);
	private SpinnerNumberModel fromSpinnerModel = new SpinnerNumberModel(1, 0, Integer.MAX_VALUE, 1);
	private SpinnerNumberModel toSpinnerModel = new SpinnerNumberModel(20, 0, Integer.MAX_VALUE, 1);
	
	private final MessageHandler messageHandler = new FileTransferableMessageHandler(this, list.getTransferablePolicy());
	
	
	public ListPanel() {
		super("List", ResourceManager.getIcon("panel.list"));
		
		list.setTitle("Title");
		
		list.setTransferablePolicy(new FileListTransferablePolicy(list));
		list.setExportHandler(new FileBotListExportHandler(list));
		
		list.getRemoveAction().setEnabled(true);
		
		JSpinner fromSpinner = new JSpinner(fromSpinnerModel);
		JSpinner toSpinner = new JSpinner(toSpinnerModel);
		
		fromSpinner.setEditor(new NumberEditor(fromSpinner, "#"));
		toSpinner.setEditor(new NumberEditor(toSpinner, "#"));
		
		setLayout(new MigLayout("nogrid, fill, insets dialog", "align center"));
		
		add(new JLabel("Pattern:"), "gapbefore indent");
		add(textField, "gap related, wmin 2cm");
		add(new JLabel("From:"), "gap 5mm");
		add(fromSpinner, "gap related, wmax 12mm, sizegroup spinner");
		add(new JLabel("To:"), "gap 5mm");
		add(toSpinner, "gap related, wmax 12mm, sizegroup spinner");
		add(new JButton(createAction), "gap 7mm, gapafter indent, wrap paragraph");
		
		add(list, "grow");
		
		// panel with buttons that will be added inside the list component
		JPanel buttonPanel = new JPanel(new MigLayout("insets 1.2mm, nogrid, fill", "align center"));
		buttonPanel.add(new JButton(new LoadAction(list.getTransferablePolicy())));
		buttonPanel.add(new JButton(new SaveAction(list.getExportHandler())), "gap related");
		
		list.add(buttonPanel, BorderLayout.SOUTH);
		
		TunedUtilities.putActionForKeystroke(this, KeyStroke.getKeyStroke("ENTER"), createAction);
	}
	

	@Override
	public MessageHandler getMessageHandler() {
		return messageHandler;
	}
	
	private AbstractAction createAction = new AbstractAction("Create") {
		
		public void actionPerformed(ActionEvent e) {
			
			int from = fromSpinnerModel.getNumber().intValue();
			int to = toSpinnerModel.getNumber().intValue();
			
			String pattern = textField.getText();
			
			if (!pattern.contains(INDEX_VARIABLE)) {
				Logger.getLogger("ui").warning(String.format("Pattern must contain index variable %s.", INDEX_VARIABLE));
				return;
			}
			
			// pad episode numbers with zeros (e.g. %02d) so all episode numbers have the same number of digits
			NumberFormat numberFormat = NumberFormat.getIntegerInstance();
			numberFormat.setMinimumIntegerDigits(max(2, Integer.toString(max(from, to)).length()));
			numberFormat.setGroupingUsed(false);
			
			List<String> names = new ArrayList<String>();
			
			for (int i = min(from, to); i <= max(from, to); i++) {
				names.add(pattern.replaceAll(Pattern.quote(INDEX_VARIABLE), numberFormat.format(i)));
			}
			
			if (signum(to - from) < 0) {
				Collections.reverse(names);
			}
			
			// try to match title from the first five names
			Collection<String> title = new SeriesNameMatcher().matchAll((names.size() < 5 ? names : names.subList(0, 4)).toArray(new String[0]));
			
			list.setTitle(title.isEmpty() ? "List" : title.iterator().next());
			
			list.getModel().clear();
			list.getModel().addAll(names);
		}
	};
	
}
