
package net.sourceforge.filebot.ui.panel.rename;


import javax.swing.event.DocumentEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;


class ExpressionFormatDocument extends PlainDocument {
	
	private Completion lastCompletion;
	

	@Override
	public void insertString(int offset, String text, AttributeSet attributes) throws BadLocationException {
		if (text == null || text.isEmpty()) {
			return;
		}
		
		// ignore user input that matches the last auto-completion
		if (lastCompletion != null && lastCompletion.didComplete(this, offset, text)) {
			lastCompletion = null;
			
			// behave as if something was inserted (e.g. update caret position)
			fireInsertUpdate(new DefaultDocumentEvent(offset, text.length(), DocumentEvent.EventType.INSERT));
			return;
		}
		
		// try to auto-complete input
		lastCompletion = Completion.getCompletion(this, offset, text);
		
		if (lastCompletion != null) {
			text = lastCompletion.complete(this, offset, text);
		}
		
		super.insertString(offset, text, attributes);
	}
	

	public Completion getLastCompletion() {
		return lastCompletion;
	}
	

	public enum Completion {
		RoundBrackets("()"),
		SquareBrackets("[]"),
		CurlyBrackets("{}"),
		SingleQuoteStringLiteral("''"),
		DoubleQuoteStringLiteral("\"\"");
		
		public final String pattern;
		

		private Completion(String pattern) {
			this.pattern = pattern;
		}
		

		public boolean canComplete(Document document, int offset, String input) {
			return pattern.startsWith(input);
		}
		

		public boolean didComplete(Document document, int offset, String input) {
			try {
				return document.getText(0, offset).concat(input).endsWith(pattern);
			} catch (BadLocationException e) {
				return false;
			}
		}
		

		public String complete(Document document, int offset, String input) {
			return pattern;
		}
		

		public static Completion getCompletion(Document document, int offset, String input) {
			for (Completion completion : values()) {
				if (completion.canComplete(document, offset, input)) {
					return completion;
				}
			}
			
			return null;
		}
		
	}
	
}
