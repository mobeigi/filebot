package net.sourceforge.filebot;

import static java.util.Arrays.*;
import static java.util.Collections.*;

import java.io.Serializable;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

public class Language implements Serializable {

	private final String iso2;
	private final String iso3;
	private final String name;

	public Language(String iso2, String iso3, String name) {
		this.iso2 = iso2;
		this.iso3 = iso3;
		this.name = name;
	}

	public String getCode() {
		return iso2;
	}

	public String getISO2() {
		return iso2;
	}

	public String getISO3() {
		return iso3;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return iso3;
	}

	public Locale getLocale() {
		return new Locale(getCode());
	}

	@Override
	public Language clone() {
		return new Language(iso2, iso3, name);
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
			String[] values = bundle.getString(code).split("\\t", 2);
			return new Language(code, values[0], values[1]);
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

	public static Language getLanguage(Locale locale) {
		if (locale != null) {
			String code = locale.getLanguage();
			for (Language it : availableLanguages()) {
				if (it.getISO2().equals(code) || it.getISO3().equals(code)) {
					return it;
				}
			}
		}
		return null;
	}

	public static Language findLanguage(String lang) {
		for (Language it : availableLanguages()) {
			if (lang.equalsIgnoreCase(it.getISO2()) || lang.equalsIgnoreCase(it.getISO3()) || lang.equalsIgnoreCase(it.getName())) {
				return it;
			}
		}
		return null;
	}

	public static String getISO3LanguageCodeByName(String languageName) {
		try {
			return Language.findLanguage(languageName).getISO3();
		} catch (Exception e) {
			return null;
		}
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
