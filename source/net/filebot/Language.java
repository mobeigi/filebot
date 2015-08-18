package net.filebot;

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

	// ISO 639-1 code
	private final String iso_639_1;

	// ISO 639-3 code (mostly identical to ISO 639-2/T)
	private final String iso_639_3;

	// ISO 639-2/B code
	private final String iso_639_2B;

	// Language name
	private final String[] names;

	public Language(String iso_639_1, String iso_639_3, String iso_639_2B, String[] names) {
		this.iso_639_1 = iso_639_1;
		this.iso_639_3 = iso_639_3;
		this.iso_639_2B = iso_639_2B;
		this.names = names.clone();
	}

	public String getCode() {
		return iso_639_1;
	}

	public String getISO2() {
		return iso_639_1; // 2-letter code
	}

	public String getISO3() {
		return iso_639_3; // 3-letter code
	}

	public String getISO3B() {
		return iso_639_2B; // alternative 3-letter code
	}

	public String getName() {
		return names[0];
	}

	public List<String> getNames() {
		return unmodifiableList(asList(names));
	}

	@Override
	public String toString() {
		return iso_639_3;
	}

	public Locale getLocale() {
		return new Locale(getCode());
	}

	public boolean matches(String code) {
		if (iso_639_1.equalsIgnoreCase(code) || iso_639_3.equalsIgnoreCase(code) || iso_639_2B.equalsIgnoreCase(code)) {
			return true;
		}
		for (String it : names) {
			if (it.equalsIgnoreCase(code) || code.toLowerCase().contains(it.toLowerCase())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Language clone() {
		return new Language(iso_639_1, iso_639_3, iso_639_2B, names);
	}

	public static final Comparator<Language> ALPHABETIC_ORDER = new Comparator<Language>() {

		@Override
		public int compare(Language o1, Language o2) {
			return o1.getName().compareToIgnoreCase(o2.getName());
		}
	};

	public static Language getLanguage(String code) {
		ResourceBundle bundle = ResourceBundle.getBundle(Language.class.getName());

		try {
			String[] values = bundle.getString(code).split("\\t", 3);
			return new Language(code, values[0], values[1], values[2].split("\\t"));
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
				if (it.matches(code)) {
					return it;
				}
			}
		}
		return null;
	}

	public static Language findLanguage(String lang) {
		for (Language it : availableLanguages()) {
			if (it.matches(lang)) {
				return it;
			}
		}
		return null;
	}

	public static String getStandardLanguageCode(String lang) {
		try {
			return Language.findLanguage(lang).getISO3();
		} catch (Exception e) {
			return null;
		}
	}

	public static List<Language> availableLanguages() {
		ResourceBundle bundle = ResourceBundle.getBundle(Language.class.getName());
		return getLanguages(bundle.getString("languages.ui").split(","));
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
