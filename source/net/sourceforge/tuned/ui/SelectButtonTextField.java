
package net.sourceforge.tuned.ui;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.border.Border;
import javax.swing.plaf.ComboBoxUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.text.JTextComponent;

import net.sourceforge.filebot.resources.ResourceManager;


public class SelectButtonTextField<T> extends JPanel {
	
	private SelectButton<T> selectButton = new SelectButton<T>();
	
	private ComboBoxTextField editor = new ComboBoxTextField();
	
	
	public SelectButtonTextField() {
		setLayout(new BorderLayout(0, 0));
		
		selectButton.addActionListener(textFieldFocusOnClick);
		
		Color borderColor = new Color(0xA4A4A4);
		
		Border lineBorder = BorderFactory.createLineBorder(borderColor, 1);
		Border matteBorder = BorderFactory.createMatteBorder(1, 0, 1, 1, borderColor);
		Border emptyBorder = BorderFactory.createEmptyBorder(0, 3, 0, 3);
		
		selectButton.setBorder(lineBorder);
		editor.setBorder(BorderFactory.createCompoundBorder(matteBorder, emptyBorder));
		
		add(editor, BorderLayout.CENTER);
		add(selectButton, BorderLayout.WEST);
		
		setPreferredSize(new Dimension(280, 22));
		
		TunedUtil.putActionForKeystroke(this, KeyStroke.getKeyStroke("shift UP"), new SpinClientAction(-1));
		TunedUtil.putActionForKeystroke(this, KeyStroke.getKeyStroke("shift DOWN"), new SpinClientAction(1));
	}
	

	/**
	 * Convenience method for <code>getEditor().getSelectedItem().toString()</code>
	 */
	public String getText() {
		return editor.getText();
	}
	

	/**
	 * Convenience method for <code>getSelectButton().getSelectedValue()</code>
	 */
	public T getSelected() {
		return getSelectButton().getSelectedValue();
	}
	

	public JComboBox getEditor() {
		return editor;
	}
	

	public SelectButton<T> getSelectButton() {
		return selectButton;
	}
	
	private final ActionListener textFieldFocusOnClick = new ActionListener() {
		
		public void actionPerformed(ActionEvent e) {
			getEditor().requestFocus();
		}
		
	};
	
	
	private class SpinClientAction extends AbstractAction {
		
		private int spin;
		
		
		public SpinClientAction(int spin) {
			this.spin = spin;
		}
		

		public void actionPerformed(ActionEvent e) {
			selectButton.spinValue(spin);
		}
	}
	

	private static class ComboBoxTextField extends JComboBox {
		
		public ComboBoxTextField() {
			setEditable(true);
			super.setUI(new TextFieldComboBoxUI());
		}
		

		@Override
		public void setUI(ComboBoxUI ui) {
			// don't reset the UI delegate if laf is changed, or we use our custom ui
		}
		

		public String getText() {
			return ((TextFieldComboBoxUI) getUI()).getEditor().getText();
		}
		

		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
			//			super.actionPerformed(e);
			//			Object newItem = getEditor().getItem();
			//			setPopupVisible(false);
			//			getModel().setSelectedItem(newItem);
			//			String oldCommand = getActionCommand();
			//			setActionCommand("comboBoxEdited");
			//			
			//TODO sysout
			System.out.println("combobox: " + e);
			//			for (ActionListener actionListener : getActionListeners()) {
			//				actionListener.actionPerformed(e);
			//			}
		}
	}
	

	private static class TextFieldComboBoxUI extends BasicComboBoxUI {
		
		@Override
		protected JButton createArrowButton() {
			return new JButton(ResourceManager.getIcon("action.list"));
		}
		

		@Override
		public void configureArrowButton() {
			super.configureArrowButton();
			
			arrowButton.setContentAreaFilled(false);
			arrowButton.setFocusable(false);
		}
		

		@Override
		protected void configureEditor() {
			editor.setEnabled(comboBox.isEnabled());
			editor.setFocusable(comboBox.isFocusable());
			editor.setFont(comboBox.getFont());
			
			editor.addFocusListener(createFocusListener());
		}
		

		public JTextComponent getEditor() {
			return (JTextComponent) editor;
		}
		
		//		@Override
		//		protected FocusListener createFocusListener() {
		//			return new FocusHandler() {
		//				
		//				/**
		//				 * Prevent action events from being fired on focusLost.
		//				 */
		//				@Override
		//				public void focusLost(FocusEvent e) {
		//					if (isPopupVisible(comboBox))
		//						setPopupVisible(comboBox, false);
		//				}
		//			};
		//		}
		
	}
	
}
