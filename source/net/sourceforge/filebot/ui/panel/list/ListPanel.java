
package net.sourceforge.filebot.ui.panel.list;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;

import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.filebot.ui.FileBotList;
import net.sourceforge.filebot.ui.FileBotPanel;
import net.sourceforge.filebot.ui.FileTransferableMessageHandler;
import net.sourceforge.filebot.ui.MessageManager;
import net.sourceforge.filebot.ui.transfer.LoadAction;
import net.sourceforge.filebot.ui.transfer.SaveAction;
import net.sourceforge.tuned.MessageBus;
import net.sourceforge.tuned.ui.TunedUtil;


public class ListPanel extends FileBotPanel {
	
	private static final String INDEX_VARIABLE = "<i>";
	
	private FileBotList list = new FileBotList(true, true, true);
	
	private SaveAction saveAction = new SaveAction(list);
	private LoadAction loadAction = new LoadAction(list.getTransferablePolicy());
	
	private JTextField textField = new JTextField(String.format("Name - %s", INDEX_VARIABLE), 25);
	private SpinnerNumberModel fromSpinnerModel = new SpinnerNumberModel(1, 0, Integer.MAX_VALUE, 1);
	private SpinnerNumberModel toSpinnerModel = new SpinnerNumberModel(20, 0, Integer.MAX_VALUE, 1);
	
	
	public ListPanel() {
		super("List", ResourceManager.getIcon("panel.list"));
		
		list.setTransferablePolicy(new FileListTransferablePolicy(list));
		
		Box buttons = Box.createHorizontalBox();
		buttons.setBorder(new EmptyBorder(5, 5, 5, 5));
		buttons.add(Box.createHorizontalGlue());
		
		buttons.add(new JButton(loadAction));
		buttons.add(Box.createHorizontalStrut(5));
		buttons.add(new JButton(saveAction));
		buttons.add(Box.createHorizontalGlue());
		
		list.add(buttons, BorderLayout.SOUTH);
		
		JSpinner fromSpinner = new JSpinner(fromSpinnerModel);
		JSpinner toSpinner = new JSpinner(toSpinnerModel);
		
		fromSpinner.setEditor(new JSpinner.NumberEditor(fromSpinner, "#"));
		toSpinner.setEditor(new JSpinner.NumberEditor(toSpinner, "#"));
		
		Dimension spinnerDimension = new Dimension(50, textField.getPreferredSize().height);
		fromSpinner.setPreferredSize(spinnerDimension);
		toSpinner.setPreferredSize(spinnerDimension);
		
		Box spinners = Box.createHorizontalBox();
		spinners.setBorder(new EmptyBorder(5, 5, 5, 5));
		spinners.add(Box.createHorizontalGlue());
		
		spinners.add(createLabeledComponent("Pattern:", textField));
		spinners.add(Box.createHorizontalStrut(15));
		spinners.add(createLabeledComponent("From:", fromSpinner));
		spinners.add(Box.createHorizontalStrut(10));
		spinners.add(createLabeledComponent("To:", toSpinner));
		spinners.add(Box.createHorizontalStrut(15));
		spinners.add(new JButton(createAction));
		spinners.add(Box.createHorizontalGlue());
		
		add(spinners, BorderLayout.NORTH);
		add(list, BorderLayout.CENTER);
		
		TunedUtil.registerActionForKeystroke(this, KeyStroke.getKeyStroke("ENTER"), createAction);
		
		MessageBus.getDefault().addMessageHandler(getPanelName(), new FileTransferableMessageHandler(getPanelName(), list.getTransferablePolicy()));
	}
	

	private JComponent createLabeledComponent(String label, JComponent component) {
		Box box = Box.createHorizontalBox();
		box.setBorder(new EmptyBorder(5, 5, 5, 5));
		box.add(new JLabel(label));
		box.add(Box.createHorizontalStrut(6));
		box.add(component);
		
		box.setMaximumSize(box.getPreferredSize());
		
		return box;
	}
	
	private AbstractAction createAction = new AbstractAction("Create") {
		
		public void actionPerformed(ActionEvent e) {
			resetList();
			
			int from = fromSpinnerModel.getNumber().intValue();
			int to = toSpinnerModel.getNumber().intValue();
			
			String pattern = textField.getText();
			
			if (!pattern.contains(INDEX_VARIABLE)) {
				MessageManager.showWarning(String.format("Pattern does not contain index variable %s.", INDEX_VARIABLE));
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
			
			list.getModel().set(entries);
		}
	};
	
	
	private void resetList() {
		list.setTitle("List");
		list.getModel().clear();
	}
}
