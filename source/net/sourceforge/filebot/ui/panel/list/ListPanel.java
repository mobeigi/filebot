
package net.sourceforge.filebot.ui.panel.list;


import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;

import net.miginfocom.swing.MigLayout;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.ui.FileBotList;
import net.sourceforge.filebot.ui.FileBotListExportHandler;
import net.sourceforge.filebot.ui.FileBotPanel;
import net.sourceforge.filebot.ui.FileTransferableMessageHandler;
import net.sourceforge.filebot.ui.transfer.LoadAction;
import net.sourceforge.filebot.ui.transfer.SaveAction;
import net.sourceforge.tuned.MessageHandler;
import net.sourceforge.tuned.ui.TunedUtil;


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
		
		fromSpinner.setEditor(new JSpinner.NumberEditor(fromSpinner, "#"));
		toSpinner.setEditor(new JSpinner.NumberEditor(toSpinner, "#"));
		
		setLayout(new MigLayout("nogrid, fill, insets 6px 2px 6px 2px", "align center"));
		
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
		
		TunedUtil.putActionForKeystroke(this, KeyStroke.getKeyStroke("ENTER"), createAction);
	}
	

	@Override
	public MessageHandler getMessageHandler() {
		return messageHandler;
	}
	
	private AbstractAction createAction = new AbstractAction("Create") {
		
		public void actionPerformed(ActionEvent e) {
			resetList();
			
			int from = fromSpinnerModel.getNumber().intValue();
			int to = toSpinnerModel.getNumber().intValue();
			
			String pattern = textField.getText();
			
			if (!pattern.contains(INDEX_VARIABLE)) {
				Logger.getLogger("ui").warning(String.format("Pattern must contain index variable %s.", INDEX_VARIABLE));
				return;
			}
			
			NumberFormat format = NumberFormat.getInstance();
			format.setGroupingUsed(false);
			
			format.setMinimumIntegerDigits(Math.max(Integer.toString(to).length(), 2));
			
			Matcher titleMatcher = Pattern.compile("^([\\w\\s]+).*(\\s+\\w*" + Pattern.quote(INDEX_VARIABLE) + ").*").matcher(pattern);
			
			if (titleMatcher.matches()) {
				list.setTitle(titleMatcher.group(1).trim());
			}
			
			ArrayList<String> entries = new ArrayList<String>();
			
			int increment = (int) Math.signum(to - from);
			int index = from;
			
			do {
				String entry = pattern.replaceAll(Pattern.quote(INDEX_VARIABLE), format.format(index));
				entries.add(entry);
				
				index += increment;
			} while (index != (to + increment));
			
			list.getModel().clear();
			list.getModel().addAll(entries);
		}
	};
	
	
	private void resetList() {
		list.setTitle("List");
		list.getModel().clear();
	}
}
