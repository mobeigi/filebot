
package net.sourceforge.filebot.ui.panel.create;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.text.NumberFormat;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;

import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.filebot.ui.FileBotPanel;
import net.sourceforge.filebot.ui.FileBotUtil;


public class CreatePanel extends FileBotPanel {
	
	private SpinnerNumberModel fromSpinnerModel = new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1);
	private SpinnerNumberModel toSpinnerModel = new SpinnerNumberModel(20, 1, Integer.MAX_VALUE, 1);
	
	private CreateList list = new CreateList();
	
	private JTextField textField = new JTextField("Name - $i", 25);
	
	
	public CreatePanel() {
		super("Create", ResourceManager.getIcon("panel.create"));
		
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
		
		JPanel panel = new JPanel(new BorderLayout());
		
		panel.add(spinners, BorderLayout.NORTH);
		panel.add(list, BorderLayout.CENTER);
		
		add(panel, BorderLayout.CENTER);
		
		FileBotUtil.registerActionForKeystroke(this, KeyStroke.getKeyStroke("ENTER"), createAction);
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
			list.getModel().clear();
			
			int from = fromSpinnerModel.getNumber().intValue();
			int to = toSpinnerModel.getNumber().intValue();
			
			NumberFormat format = NumberFormat.getInstance();
			format.setGroupingUsed(false);
			
			format.setMinimumIntegerDigits(Math.max(Integer.toString(to).length(), 2));
			
			String pattern = textField.getText();
			
			for (int i = from; i <= to; i++) {
				String s = pattern.replaceAll("\\$i", format.format(i));
				list.getModel().addElement(s);
			}
		}
	};
	
}
