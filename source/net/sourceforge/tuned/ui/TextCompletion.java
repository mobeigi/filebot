
package net.sourceforge.tuned.ui;


import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;


public class TextCompletion {
	
	private Set<String> completionTerms = Collections.synchronizedSet(new TreeSet<String>(String.CASE_INSENSITIVE_ORDER));;
	
	private int completionStartLength = 1;
	
	private JTextComponent component;
	
	
	public TextCompletion(JTextComponent component) {
		this.component = component;
	}
	

	public void hook() {
		component.getDocument().addDocumentListener(documentListener);
	}
	

	public void unhook() {
		component.getDocument().removeDocumentListener(documentListener);
	}
	

	public void addCompletionTerm(String term) {
		completionTerms.add(term);
	}
	

	public void addCompletionTerms(Collection<String> terms) {
		completionTerms.addAll(terms);
	}
	

	public void removeCompletionTerm(String term) {
		completionTerms.remove(term);
	}
	

	public void removeCompletionTerms(Collection<String> terms) {
		completionTerms.removeAll(terms);
	}
	

	public void setCompletionStartLength(int codeCompletionStartLength) {
		this.completionStartLength = codeCompletionStartLength;
	}
	

	public int getCompletionStartLength() {
		return completionStartLength;
	}
	

	private void complete() {
		String text = component.getText();
		
		if (text.length() < completionStartLength)
			return;
		
		String completionTerm = findCompletionTerm(text);
		
		if (completionTerm == null)
			return;
		
		component.setText(completionTerm);
		component.select(text.length(), completionTerm.length());
	}
	

	private String findCompletionTerm(String text) {
		for (String completionTerm : completionTerms) {
			if (text.length() >= completionTerm.length())
				continue;
			
			String compareTerm = completionTerm.substring(0, text.length());
			
			if (text.equalsIgnoreCase(compareTerm))
				return completionTerm;
		}
		
		return null;
	}
	
	private final DocumentListener documentListener = new DocumentListener() {
		
		public void changedUpdate(DocumentEvent e) {
		}
		

		public void insertUpdate(DocumentEvent e) {
			SwingUtilities.invokeLater(doComplete);
		}
		

		public void removeUpdate(DocumentEvent e) {
			
		}
		
	};
	
	private final Runnable doComplete = new Runnable() {
		
		public void run() {
			complete();
		}
	};
	
}
