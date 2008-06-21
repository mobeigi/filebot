
package net.sourceforge.filebot.ui.panel.subtitle;


import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.matchers.AbstractMatcherEditor;
import ca.odell.glazedlists.matchers.Matcher;


public class LanguageMatcherEditor extends AbstractMatcherEditor<SubtitlePackage> {
	
	private final EventList<Language> languages;
	
	
	public LanguageMatcherEditor(LanguageSelectionPanel languageSelectionPanel) {
		this(languageSelectionPanel.getSelected());
		currentMatcher = new LanguageMatcher(this.languages);
	}
	

	public LanguageMatcherEditor(EventList<Language> languages) {
		this.languages = languages;
		
		languages.addListEventListener(new LanguageSelectionListener());
	}
	
	
	private class LanguageSelectionListener implements ListEventListener<Language> {
		
		@Override
		public void listChanged(ListEvent<Language> listChanges) {
			boolean insert = false;
			boolean delete = false;
			
			while (listChanges.next()) {
				int type = listChanges.getType();
				
				insert |= (type == ListEvent.INSERT);
				delete |= (type == ListEvent.DELETE);
			}
			
			if (insert || delete) {
				Matcher<SubtitlePackage> matcher = new LanguageMatcher(languages);
				
				if (insert && !delete) {
					fireRelaxed(matcher);
				} else if (!insert && delete) {
					fireConstrained(matcher);
				} else {
					fireChanged(matcher);
				}
			}
		}
	}
	
}
