
package net.sourceforge.tuned.ui;


import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.text.JTextComponent;

import net.miginfocom.swing.MigLayout;
import net.sourceforge.filebot.ResourceManager;


public class SelectButtonTextField<T> extends JComponent {
	
	private SelectButton<T> selectButton = new SelectButton<T>();
	
	private JComboBox editor = new JComboBox();
	
	
	public SelectButtonTextField() {
		selectButton.addActionListener(textFieldFocusOnClick);
		
		editor.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 1, ((LineBorder) selectButton.getBorder()).getLineColor()));
		
		setLayout(new MigLayout("nogrid, fill"));
		add(selectButton, "h pref!, w pref!, sizegroupy this");
		add(editor, "gap 0, w 195px!, sizegroupy this");
		
		editor.setUI(new TextFieldComboBoxUI());
		
		TunedUtilities.putActionForKeystroke(this, KeyStroke.getKeyStroke("ctrl UP"), new SpinClientAction(-1));
		TunedUtilities.putActionForKeystroke(this, KeyStroke.getKeyStroke("ctrl DOWN"), new SpinClientAction(1));
	}
	

	public String getText() {
		return ((TextFieldComboBoxUI) editor.getUI()).getEditor().getText();
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
	

	private class TextFieldComboBoxUI extends BasicComboBoxUI {
		
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
			JTextComponent editor = getEditor();
			
			editor.setEnabled(comboBox.isEnabled());
			editor.setFocusable(comboBox.isFocusable());
			editor.setFont(comboBox.getFont());
			editor.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
			
			editor.addFocusListener(createFocusListener());
		}
		

		public JTextComponent getEditor() {
			return (JTextComponent) editor;
		}
		

		@Override
		protected ComboPopup createPopup() {
			return new BasicComboPopup(comboBox) {
				
				@Override
				public void show(Component invoker, int x, int y) {
					super.show(invoker, x - selectButton.getWidth(), y);
				}
				

				@Override
				protected Rectangle computePopupBounds(int px, int py, int pw, int ph) {
					Rectangle bounds = super.computePopupBounds(px, py, pw, ph);
					bounds.width += selectButton.getWidth();
					
					return bounds;
				}
			};
		}
		

		@Override
		protected FocusListener createFocusListener() {
			return new FocusHandler() {
				
				/**
				 * Prevent action events from being fired on focusLost.
				 */
				@Override
				public void focusLost(FocusEvent e) {
					if (isPopupVisible(comboBox))
						setPopupVisible(comboBox, false);
				}
			};
		}
		
	}
	
}
