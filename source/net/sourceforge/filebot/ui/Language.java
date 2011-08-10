
package net.sourceforge.filebot.ui;


import static java.util.Arrays.*;
import static java.util.Collections.*;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
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
			return new Language(code, bundle.getString(code + ".name"));
		} catch (Exception e) {
			return null;
		}
	}
	

	public static List<Language> getLanguages(String... codes) {
		Language[] languages = new Language[codes.length];
		
		for (int i = 0; i < codes.length; i++) {
			languages[i] = getLanguage(codes[i]);
		}
		
		return asList(languages);
	}
	

	public static List<Language> availableLanguages() {
		ResourceBundle bundle = ResourceBundle.getBundle(Language.class.getName());
		return getLanguages(bundle.getString("languages.all").split(","));
	}
	

	public static List<Language> commonLanguages() {
		ResourceBundle bundle = ResourceBundle.getBundle(Language.class.getName());
		return getLanguages(bundle.getString("languages.common").split(","));
	}
	

	public static List<Language> preferredLanguages() {
		Set<String> codes = new LinkedHashSet<String>();
		
		// English | System language | common languages
		codes.add("en");
		codes.add(Locale.getDefault().getLanguage());
		
		ResourceBundle bundle = ResourceBundle.getBundle(Language.class.getName());
		addAll(codes, bundle.getString("languages.common").split(","));
		
		return getLanguages(codes.toArray(new String[0]));
	}
}
