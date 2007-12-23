
package net.sourceforge.tuned.ui;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;


public class TextFieldWithSelect<T> extends JPanel {
	
	private SelectButton<T> selectButton;
	
	private JTextField textfield = new JTextField();
	private Color borderColor = Color.decode("#A4A4A4");
	
	
	public TextFieldWithSelect(List<T> options, Map<T, ? extends Icon> icons) {
		setLayout(new BorderLayout(0, 0));
		
		selectButton = new SelectButton<T>(options, icons);
		selectButton.addActionListener(textFieldFocusOnClick);
		
		Border lineBorder = BorderFactory.createLineBorder(borderColor, 1);
		Border matteBorder = BorderFactory.createMatteBorder(1, 0, 1, 1, borderColor);
		Border emptyBorder = BorderFactory.createEmptyBorder(0, 3, 0, 3);
		
		selectButton.setBorder(lineBorder);
		textfield.setBorder(BorderFactory.createCompoundBorder(matteBorder, emptyBorder));
		
		textfield.setColumns(20);
		add(textfield, BorderLayout.CENTER);
		add(selectButton, BorderLayout.WEST);
	}
	

	public JTextField getTextField() {
		return textfield;
	}
	

	public SelectButton<T> getSelectButton() {
		return selectButton;
	}
	

	public T getSelectedValue() {
		return selectButton.getSelectedValue();
	}
	

	public void clearTextSelection() {
		int length = textfield.getText().length();
		textfield.select(length, length);
	}
	
	private final ActionListener textFieldFocusOnClick = new ActionListener() {
		
		public void actionPerformed(ActionEvent e) {
			textfield.requestFocus();
		}
		
	};
	
}
