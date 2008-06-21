
package net.sourceforge.filebot.ui.panel.subtitle;


import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import ca.odell.glazedlists.matchers.Matcher;


class LanguageMatcher implements Matcher<SubtitlePackage> {
	
	private final Set<Language> languages;
	
	
	public LanguageMatcher(Collection<Language> languages) {
		this.languages = new TreeSet<Language>(languages);
	}
	

	@Override
	public boolean matches(SubtitlePackage item) {
		return languages.contains(item.getLanguage());
	}
	
}
