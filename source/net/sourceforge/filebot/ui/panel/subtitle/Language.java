
package net.sourceforge.filebot.ui.panel.subtitle;


import java.util.Comparator;
import java.util.ResourceBundle;
import java.util.Set;


public class Language {
	
	private final String code;
	private final String name;
	

	public Language(String code, String name) {
		this.code = code;
		this.name = name;
	}
	

	public String getCode() {
		return code;
	}
	

	public String getName() {
		return name;
	}
	

	@Override
	public String toString() {
		return name;
	}
	

	@Override
	public Language clone() {
		return new Language(code, name);
	}
	

	public static final Comparator<Language> ALPHABETIC_ORDER = new Comparator<Language>() {
		
		@Override
		public int compare(Language o1, Language o2) {
			return o1.name.compareToIgnoreCase(o2.name);
		}
	};
	

	public static Language getLanguage(String code) {
		ResourceBundle bundle = ResourceBundle.getBundle(Language.class.getName());
		
		try {
			return new Language(code, bundle.getString(code));
		} catch (Exception e) {
			return null;
		}
	}
	

	public static Language[] availableLanguages() {
		ResourceBundle bundle = ResourceBundle.getBundle(Language.class.getName());
		Set<String> languageCodeSet = bundle.keySet();
		
		Language[] languages = new Language[languageCodeSet.size()];
		int size = 0;
		
		// fill languages array
		for (String code : languageCodeSet) {
			languages[size++] = new Language(code, bundle.getString(code));
		}
		
		return languages;
	}
	
}
