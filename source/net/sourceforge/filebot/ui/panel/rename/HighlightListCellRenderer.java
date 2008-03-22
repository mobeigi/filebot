
package net.sourceforge.filebot.ui.panel.rename;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Insets;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;

import net.sourceforge.tuned.ui.AbstractFancyListCellRenderer;


public class HighlightListCellRenderer extends AbstractFancyListCellRenderer {
	
	private final JTextComponent textComponent = new JTextField();
	
	private Pattern pattern;
	private Highlighter.HighlightPainter highlightPainter;
	
	
	public HighlightListCellRenderer(Pattern pattern, Highlighter.HighlightPainter highlightPainter, int padding) {
		super(new Insets(padding, padding, padding, padding));
		
		this.pattern = pattern;
		this.highlightPainter = highlightPainter;
		
		textComponent.setBorder(null);
		textComponent.setOpaque(false);
		
		this.add(textComponent, BorderLayout.WEST);
		
		textComponent.getDocument().addDocumentListener(new HighlightUpdateListener());
	}
	

	@Override
	protected void configureListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		super.configureListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		
		textComponent.setText(value.toString());
	}
	

	protected void updateHighlighter() {
		textComponent.getHighlighter().removeAllHighlights();
		
		Matcher matcher = pattern.matcher(textComponent.getText());
		
		while (matcher.find()) {
			try {
				textComponent.getHighlighter().addHighlight(matcher.start(0), matcher.end(0), highlightPainter);
			} catch (BadLocationException e) {
				//should not happen
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, e.toString(), e);
			}
		}
	}
	

	@Override
	public void setForeground(Color fg) {
		super.setForeground(fg);
		
		// textComponent is null while in super constructor
		if (textComponent != null) {
			textComponent.setForeground(fg);
		}
	}
	
	
	private class HighlightUpdateListener implements DocumentListener {
		
		@Override
		public void changedUpdate(DocumentEvent e) {
			
		}
		

		@Override
		public void insertUpdate(DocumentEvent e) {
			updateHighlighter();
		}
		

		@Override
		public void removeUpdate(DocumentEvent e) {
			updateHighlighter();
		}
		
	}
	
}
