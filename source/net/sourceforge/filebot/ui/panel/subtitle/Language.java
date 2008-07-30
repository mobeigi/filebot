
package net.sourceforge.filebot.ui.panel.subtitle;


import java.util.Locale;

import javax.swing.Icon;

import net.sourceforge.filebot.resources.ResourceManager;


class Language implements Comparable<Language> {
	
	private final String name;
	private final Locale locale;
	
	private final String code;
	private final Icon icon;
	
	
	public Language(String languageName) {
		this.name = languageName;
		this.locale = LanguageResolver.getDefault().getLocale(name);
		
		this.code = (locale != null ? locale.getLanguage() : null);
		
		this.icon = ResourceManager.getFlagIcon(code);
	}
	

	public String getName() {
		return name;
	}
	

	public String getCode() {
		return code;
	}
	

	public Locale getLocale() {
		return locale;
	}
	

	public Icon getIcon() {
		return icon;
	}
	

	@Override
	public String toString() {
		return getName();
	}
	

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		
		if (obj instanceof Language)
			return getName().equalsIgnoreCase(((Language) obj).getName());
		
		return false;
	}
	

	@Override
	public int compareTo(Language language) {
		return getName().compareToIgnoreCase(language.getName());
	}
	
}
